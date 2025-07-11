package email_connector.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.CoreRuntimeException;
import com.mendix.datahub.connector.email.model.Message;
import com.mendix.datahub.connector.email.service.OnFetchEmailListener;
import com.mendix.datahub.connector.email.utils.EmailConnectorException;
import com.mendix.datahub.connector.eventtracking.Metrics;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import email_connector.proxies.EmailAccount;
import email_connector.proxies.constants.Constants;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static com.mendix.core.Core.microflowCall;

public class EmailListener implements OnFetchEmailListener {
    private static final ILogNode log = Core.getLogger(Constants.getLogNode());
    private final String batchReceivedMicroflow;
    private final String batchCompleteMicroflow;
    private final String batchFailedMicroflow;
    private final EmailAccount emailAccount;

    public EmailListener(String batchReceivedMicroflow, String batchCompleteMicroflow, String batchFailedMicroflow, EmailAccount mxEmailAccount) {
        this.batchReceivedMicroflow = batchReceivedMicroflow;
        this.batchCompleteMicroflow = batchCompleteMicroflow;
        this.batchFailedMicroflow = batchFailedMicroflow;
        this.emailAccount = mxEmailAccount;
    }

    @Override
    public void onBatchReceived(List<Message> messageList) {
        IContext context = null;
        try {
            Metrics.createCounter("dnl_connectors_ec_receive_email")
                    .addTag("email_protocol", this.emailAccount.getIncomingEmailConfiguration_EmailAccount().getIncomingProtocol().name())
                    .setDescription("App receives email")
                    .build()
                    .increment(messageList.size());
        } catch (CoreException e) {
            log.error(e);
        }
        try {
            context = Core.createSystemContext();
            context.startTransaction();
            var emails = MxMailMapper.mapEmails(emailAccount, messageList, context);
            emails = Core.commit(context, emails);
            context.endTransaction();
            microflowCall(batchReceivedMicroflow)
                    .inTransaction(true)
                    .withParam("EmailMessageList", emails)
                    .execute(context);
        } catch (CoreRuntimeException | EmailConnectorException | CoreException e) {
            log.error(e.getMessage(), e);
            if (context != null && context.isInTransaction())
                context.rollbackTransaction();
        }
    }

    @Override
    public void onBatchComplete() {
        log.debug("Batch fetching completed!");
        microflowCall(batchCompleteMicroflow)
                .execute(Core.createSystemContext());
    }

    @Override
    public void onBatchFailed(Exception e) {
        log.error("Batch fetching failed!", e);
        var sStackTrace = "";
        try (var sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            sStackTrace = sw.toString();
        } catch (IOException ignored) { //NOSONAR

        }
        microflowCall(batchFailedMicroflow).withParam("Message", e.getMessage())
                .withParam("ErrorType", "BatchFailedException")
                .withParam("StackTrace", sStackTrace)
                .withParam("EmailAccount", this.emailAccount.getMendixObject())
                .execute(Core.createSystemContext());
    }
}
