package documentgeneration.implementation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.thirdparty.org.json.JSONException;
import com.mendix.thirdparty.org.json.JSONObject;

import documentgeneration.implementation.exceptions.DocGenException;
import documentgeneration.implementation.exceptions.DocGenNavigationException;
import documentgeneration.implementation.exceptions.DocGenRuntimeException;
import documentgeneration.implementation.exceptions.DocGenWaitForContentException;
import documentgeneration.proxies.DocumentRequest;

public class DocumentRequestErrorManager {

	public static RuntimeException throwDocumentRequestException(DocumentRequest documentRequest) {
		String errorMessage = generateErrorMessage(documentRequest);

		switch (DocGenServiceErrorCodes.valueOfCode(documentRequest.getErrorCode())) {
		case DOCGEN_NAVIGATION:
			throw new DocGenNavigationException(errorMessage);
		case DOCGEN_WAIT_CONTENT:
			throw new DocGenWaitForContentException(errorMessage);
		case DOCGEN_RUNTIME:
			throw new DocGenRuntimeException(errorMessage);
		default:
			throw new DocGenException(errorMessage);
		}
	}

	public static boolean handleDocumentRequestError(DocumentRequest documentRequest, IMxRuntimeRequest request) {
		try {
			storeErrorInDocumentRequest(documentRequest, request);
		} catch (Exception e) {
			logging.error("An error occured while handling the error callback for document request: "
					+ documentRequest.getRequestId() + "\n" + e.getMessage());
			return false;
		}

		logging.debug("Received and stored error code '" + documentRequest.getErrorCode() + "' and message '"
				+ documentRequest.getErrorMessage() + "' for document request: " + documentRequest.getRequestId());

		return true;
	}

	private static String generateErrorMessage(DocumentRequest documentRequest) {
		String message = "Failed to generate document for request: " + documentRequest.getRequestId();

		if (documentRequest.getErrorCode() != null)
			message += "\n" + documentRequest.getErrorCode() + ": " + documentRequest.getErrorMessage();

		return message;
	}

	private static void storeErrorInDocumentRequest(DocumentRequest documentRequest, IMxRuntimeRequest request)
			throws IOException, CoreException, JSONException {
		JSONObject requestBody = parseRequestBody(request);

		documentRequest.setErrorCode(requestBody.getString("code"));
		documentRequest.setErrorMessage(requestBody.getString("message"));
		documentRequest.commit(Core.createSystemContext());
	}

	private static JSONObject parseRequestBody(IMxRuntimeRequest request) throws IOException, JSONException {
		try (InputStream is = request.getInputStream()) {
			String rawResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return new JSONObject(rawResponse);
		}
	}

	private enum DocGenServiceErrorCodes {
		DOCGEN_NAVIGATION("DOCGEN_NAVIGATION_ERROR"), DOCGEN_WAIT_CONTENT("DOCGEN_FOR_WAIT_CONTENT_ERROR"),
		DOCGEN_RUNTIME("DOCGEN_RUNTIME_ERROR"), DOCGEN_DEFAULT("DOCGEN_DEFAULT_ERROR");

		public final String errorCode;

		private DocGenServiceErrorCodes(String errorCode) {
			this.errorCode = errorCode;
		}

		public static DocGenServiceErrorCodes valueOfCode(String errorCode) {
			for (DocGenServiceErrorCodes e : values()) {
				if (e.errorCode.equals(errorCode)) {
					return e;
				}
			}
			return DocGenServiceErrorCodes.DOCGEN_DEFAULT;
		}
	};

	private static final ILogNode logging = Logging.logNode;
}
