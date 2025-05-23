package documentgeneration.implementation;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import documentgeneration.implementation.exceptions.DocGenPollingException;
import documentgeneration.proxies.DocumentRequest;
import documentgeneration.proxies.Enum_DocumentRequest_Status;
import system.proxies.FileDocument;

interface RequestInfo {
	boolean canContinue();
}

public class RequestManager {
	private static final ConcurrentMap<String, Thread> pendingWaits = new ConcurrentHashMap<>();

	public static IMendixObject waitForResult(IWaitStrategy waitStrategy, String requestId) {
		pendingWaits.put(requestId, Thread.currentThread());
		try {
			for (int i = 0; waitStrategy.canContinue(); i++) {
				Optional<DocumentRequest> requestObject = loadFinalizedDocumentRequest(requestId);
				if (requestObject.isPresent()) {
					logging.trace("Received document result for request " + requestId);
					return processResult(requestObject.get());
				}

				logging.trace("Document result is not yet available, continue polling");
				waitStrategy.wait(i);
			}
			logging.trace("Document result has not appeared, stopping polling");
		} catch (InterruptedException e) {
			Optional<DocumentRequest> requestObject = loadFinalizedDocumentRequest(requestId);
			if (requestObject.isPresent()) {
				logging.trace("Interrupted polling, document result is available for request " + requestId);
				return processResult(requestObject.get());
			}
		} finally {
			pendingWaits.remove(requestId);
		}

		failRequest(requestId);
		throw new DocGenPollingException("Timeout while waiting for document result for request " + requestId);
	}

	public static void interruptPendingRequest(String requestId) {
		Thread waitingThread = pendingWaits.get(requestId);
		if (waitingThread != null) {
			waitingThread.interrupt();
		}
	}

	private static Optional<DocumentRequest> loadFinalizedDocumentRequest(String requestId) {
		String query = "//DocumentGeneration.DocumentRequest[RequestId=$RequestId][Status = 'Completed' or Status = 'Failed']";
		IContext systemContext = Core.createSystemContext();

		return Core.createXPathQuery(query).setVariable("RequestId", requestId).execute(systemContext).stream()
				.map(obj -> DocumentRequest.initialize(systemContext, obj)).findAny();
	}

	private static IMendixObject processResult(DocumentRequest documentRequest) {
		if (documentRequest.getStatus().equals(Enum_DocumentRequest_Status.Completed)) {
			FileDocument fileDocument = DocumentRequestManager.getFileDocument(documentRequest);
			if (fileDocument == null)
				throw new RuntimeException("File document not found");

			return fileDocument.getMendixObject();
		} else if (documentRequest.getStatus().equals(Enum_DocumentRequest_Status.Failed)) {
			if (documentRequest.getErrorCode() != null)
				DocumentRequestErrorManager.throwDocumentRequestException(documentRequest);

			throw new RuntimeException("Failed to generate document");
		} else {
			throw new RuntimeException("Invalid document request status");
		}
	}

	private static void failRequest(String requestId) {
		DocumentRequest documentRequest = DocumentRequestManager.loadDocumentRequest(requestId,
				Core.createSystemContext());
		try {
			DocumentRequestManager.failDocumentRequest(documentRequest);
		} catch (CoreException e) {
			logging.error("Could not update status for request " + requestId);
		}
	}

	private static final ILogNode logging = Logging.logNode;
}