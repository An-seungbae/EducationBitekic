package email_connector.implementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.datahub.connector.email.model.Attachment;
import com.mendix.datahub.connector.email.model.LDAPConfiguration;
import com.mendix.datahub.connector.email.model.*;
import com.mendix.datahub.connector.email.model.autoconfig.EmailProvider;
import com.mendix.datahub.connector.email.utils.EmailConnectorException;
import com.mendix.datahub.connector.email.utils.Error;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import email_connector.proxies.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static email_connector.implementation.Commons.*;

public class MxMailMapper {

    static final String ADDRESSSEPARATORREGEX = "\\s*;\\s*";

    private MxMailMapper() {
    }

    public static List<IMendixObject> mapEmails(EmailAccount mxEmailAccount, List<Message> serverEmailList, IContext context) throws EmailConnectorException, CoreException {

        ArrayList<IMendixObject> mxMailList = new ArrayList<>();
        for (Message serverMessage : serverEmailList) {
            var mxEmailMessage = getMxEmailMessage(mxEmailAccount, serverMessage, context);
            mxMailList.add(mxEmailMessage.getMendixObject());
        }
        return mxMailList;
    }

    public static void setReceiveAccountConfigurations(EmailAccount mxEmailAccount, ReceiveEmailAccount serverAccount) throws CoreException, EmailConnectorException {
        serverAccount.setFolder(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getFolder());
        serverAccount.setBatchSize(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getBatchSize());
        serverAccount.setHandling(getHandling(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getHandling()));
        serverAccount.setMoveFolder(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getMoveFolder());
        serverAccount.setUseInlineImages(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getProcessInlineImage());
        serverAccount.setUseBatchImport(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getUseBatchImport());
        serverAccount.setFetchStrategy(getFetchStrategy(mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getFetchStrategy()));
        setCommonAccountConfiguration(mxEmailAccount, serverAccount);

    }

    private static void setCommonAccountConfiguration(EmailAccount mxEmailAccount, Account serverAccount) throws EmailConnectorException {
        serverAccount.setSanitizeEmailBodyForXSSScript(mxEmailAccount.getsanitizeEmailBodyForXSSScript());
        serverAccount.setConnectionTimeout(String.valueOf(mxEmailAccount.getTimeout()));
        serverAccount.setFromDisplayName(mxEmailAccount.getFromDisplayName());
        serverAccount.setUseSSLServerCheckIdentity(mxEmailAccount.getUseSSLCheckServerIdentity());
        serverAccount.setSharedMailbox(mxEmailAccount.getIsSharedMailbox());
        if (mxEmailAccount.getIsSharedMailbox() && (mxEmailAccount.getMailAddress() == null || mxEmailAccount.getMailAddress().isBlank())) {
            throw new EmailConnectorException(Error.EMPTY_MAIL_ADDRESS.getMessage());
        }
        serverAccount.setMailAddress(mxEmailAccount.getMailAddress());
        serverAccount.setOAuthUsed(mxEmailAccount.getisOAuthUsed());
        if (serverAccount.isOAuthUsed()) {
            serverAccount.setoAuthAccessTokenWorker(new OAuthTokenWorker(mxEmailAccount));
        }
    }

    private static void processAttachment(Message serverMessage, EmailMessage mxEmailMessage, boolean processInlineAttachment, IContext context) throws EmailConnectorException {
        List<Attachment> serverAttachmentList = serverMessage.getAttachment();
        for (Attachment serverAttachment : serverAttachmentList) {
            var mxAttachment = new email_connector.proxies.Attachment(context);
            mxAttachment.setHasContents(true);
            mxAttachment.setAttachment_EmailMessage(mxEmailMessage);
            mxAttachment.setContentID(serverAttachment.getAttachmentContentID());
            mxAttachment.setName(serverAttachment.getAttachmentName());
            mxAttachment.setSize(Long.valueOf(serverAttachment.getAttachmentSize()));
            mxAttachment.setattachmentContentType(serverAttachment.getAttachmentType());
            mxAttachment.setattachmentName(serverAttachment.getAttachmentName());
            mxAttachment.setattachmentSize(serverAttachment.getAttachmentSize());
            if (serverAttachment.getAttachmentPosition() != null)
                mxAttachment.setPosition(getPosition(serverAttachment.getAttachmentPosition()));
            var byteStream = new ByteArrayInputStream(serverAttachment.getAttachmentContent());
            Core.storeFileDocumentContent(context, mxAttachment.getMendixObject(), byteStream);
            if (serverAttachment.getAttachmentPosition() != null && processInlineAttachment) {
                displayInlineAttachment(mxEmailMessage, serverAttachment, mxAttachment);
            }
        }
    }

    private static void displayInlineAttachment(EmailMessage mxEmailMessage, Attachment serverAttachment, email_connector.proxies.Attachment mxAttachment) {
        var url = "/file?target=window&guid=";
        if (serverAttachment.getAttachmentPosition().equalsIgnoreCase("inline")) {
            String source = "cid:" + serverAttachment.getAttachmentContentID();
            String target = url + mxAttachment.getMendixObject().getId().toLong();
            mxEmailMessage.setContent(mxEmailMessage.getContent().replaceAll(source, target));
        }
    }

    private static EmailMessage getMxEmailMessage(EmailAccount mxEmailAccount, Message serverMessage, IContext context) throws EmailConnectorException, CoreException {
        var mxEmailMessage = new EmailMessage(context);
        mxEmailMessage.setEmailMessage_EmailAccount(mxEmailAccount);
        mxEmailMessage.setSubject(serverMessage.getSubject());
        mxEmailMessage.setSentDate(serverMessage.getSentDate());
        mxEmailMessage.setFrom(String.join(";", serverMessage.getFrom()));
        mxEmailMessage.setTo(String.join(";", serverMessage.getTo()));
        mxEmailMessage.setCC(String.join(";", serverMessage.getCc()));
        mxEmailMessage.setBCC(String.join(";", serverMessage.getBcc()));
        mxEmailMessage.setContent(serverMessage.getContent());
        mxEmailMessage.setPlainBody(serverMessage.getPlainBody());
        mxEmailMessage.setUseOnlyPlainText(serverMessage.getUseOnlyPlainText());
        mxEmailMessage.sethasAttachments(serverMessage.isHasAttachments());
        mxEmailMessage.setStatus(ENUM_EmailStatus.RECEIVED);
        mxEmailMessage.setReplyTo(serverMessage.getReplyTo());
        mxEmailMessage.setFromDisplayName(serverMessage.getFromDisplayName());
        if (serverMessage.isHasAttachments()) {
            processAttachment(serverMessage, mxEmailMessage, mxEmailAccount.getIncomingEmailConfiguration_EmailAccount().getProcessInlineImage(), context);
        }
        if (serverMessage.getHeaders() != null && !serverMessage.getHeaders().isEmpty()) {
            for (var header : serverMessage.getHeaders().entrySet()) {
                var mxHeader = new EmailHeader(context);
                mxHeader.setKey(header.getKey());
                mxHeader.setValue(header.getValue());
                mxHeader.setEmailHeader_EmailMessage(mxEmailMessage);
                mxHeader.commit();
            }
        }
        mxEmailMessage.setSize(serverMessage.getSize());
        return mxEmailMessage;
    }

    public static void setSendAccountConfigurations(EmailAccount mxEmailAccount, SendEmailAccount serverAccount, Pk12Certificate pkcsCertficate, email_connector.proxies.LDAPConfiguration mxLdapConfiguration, IContext context) throws CoreException, EmailConnectorException {
        serverAccount.setUseSSLSMTP(mxEmailAccount.getOutgoingEmailConfiguration_EmailAccount().getSSL());
        serverAccount.setUseTLSSMTP(mxEmailAccount.getOutgoingEmailConfiguration_EmailAccount().getTLS());
        if (pkcsCertficate != null) {
            try (
                    var inputStream = Core.getFileDocumentContent(context, pkcsCertficate.getMendixObject());
                    var byteArrayOutputStream = new ByteArrayOutputStream()
            ) {
                inputStream.transferTo(byteArrayOutputStream);
                serverAccount.setP12Certificate(byteArrayOutputStream.toByteArray());
                serverAccount.setPassphrase(pkcsCertficate.getPassphrase());
            } catch (IOException ex) {
                throw new EmailConnectorException(Error.ERROR_FETCH_CERTIFICATE_CONTENT.getMessage());
            }
        }
        if (mxLdapConfiguration != null) {
            var ldapConfigurations = new LDAPConfiguration();
            ldapConfigurations.setHost(mxLdapConfiguration.getLDAPHost());
            ldapConfigurations.setPort(mxLdapConfiguration.getLDAPPort());
            ldapConfigurations.setSearchBaseName(mxLdapConfiguration.getBaseDN());
            ldapConfigurations.setSSL(mxLdapConfiguration.getisSSL());
            ldapConfigurations.setUserName(mxLdapConfiguration.getLDAPUsername());
            ldapConfigurations.setPassword(mxLdapConfiguration.getLDAPPassword());
            serverAccount.setLdapConfigurations(ldapConfigurations);
        }
        setCommonAccountConfiguration(mxEmailAccount, serverAccount);
    }

    public static void setServerSendEmailMessage(EmailMessage emailMessage, SendEmailMessage sendEmailMsg, List<email_connector.proxies.Attachment> attachmentList, IContext context) throws EmailConnectorException {
        sendEmailMsg.setSubject(emailMessage.getSubject());
        sendEmailMsg.setFromDisplayName(emailMessage.getFromDisplayName());
        setRecipients(emailMessage, sendEmailMsg);
        sendEmailMsg.setContent(emailMessage.getContent());
        sendEmailMsg.setPlainBody(emailMessage.getPlainBody());
        sendEmailMsg.setUseOnlyPlainText(emailMessage.getUseOnlyPlainText());
        sendEmailMsg.setSigned(emailMessage.getisSigned());
        sendEmailMsg.setEncrypted(emailMessage.getisEncrypted());
        Map<String, String> headerMap = new HashMap<>();
        List<IMendixObject> emailHeaders = Core.retrieveByPath(context, emailMessage.getMendixObject(), EmailHeader.MemberNames.EmailHeader_EmailMessage.toString());
        if (emailHeaders != null) {
            for (IMendixObject header : emailHeaders) {
                headerMap.put(EmailHeader.initialize(context, header).getKey(), EmailHeader.initialize(context, header).getValue());
            }
        }

        sendEmailMsg.setHeaders(headerMap);

        if (emailMessage.getReplyTo() != null && !emailMessage.getReplyTo().isEmpty())
            sendEmailMsg.setReplyTo(emailMessage.getReplyTo());
        if (attachmentList != null && !attachmentList.isEmpty()) {
            List<Attachment> serverAttachmentList = new ArrayList<>();
            for (email_connector.proxies.Attachment mxAttachment : attachmentList) {
                var attachment = new Attachment();
                attachment.setAttachmentName(mxAttachment.getName());
                try (

                        var inputStream = Core.getFileDocumentContent(context, mxAttachment.getMendixObject());
                        var byteArrayOutputStream = new ByteArrayOutputStream()
                ) {
                    inputStream.transferTo(byteArrayOutputStream);
                    attachment.setAttachmentContent(byteArrayOutputStream.toByteArray());
                    attachment.setAttachmentPosition(mxAttachment.getPosition() != null ? mxAttachment.getPosition().getCaption() : "Attachment");
                    serverAttachmentList.add(attachment);
                } catch (IOException ex) {
                    throw new EmailConnectorException(Error.ERROR_FETCH_ATTACHMENT_CONTENT.getMessage());
                }
            }
            sendEmailMsg.setAttachment(serverAttachmentList);
        }
    }

    private static void setRecipients(EmailMessage emailMessage, SendEmailMessage sendEmailMsg) {
        if (emailMessage.getFrom() != null && !emailMessage.getFrom().isEmpty())
            sendEmailMsg.setFrom(Arrays.asList(emailMessage.getFrom().trim().split(ADDRESSSEPARATORREGEX)));
        if (emailMessage.getTo() != null && !emailMessage.getTo().isEmpty())
            sendEmailMsg.setTo(Arrays.asList(emailMessage.getTo().trim().split(ADDRESSSEPARATORREGEX)));
        if (emailMessage.getCC() != null && !emailMessage.getCC().isEmpty())
            sendEmailMsg.setCc(Arrays.asList(emailMessage.getCC().trim().split(ADDRESSSEPARATORREGEX)));
        if (emailMessage.getBCC() != null && !emailMessage.getBCC().isEmpty())
            sendEmailMsg.setBcc(Arrays.asList(emailMessage.getBCC().trim().split(ADDRESSSEPARATORREGEX)));
    }

    public static void getMappedEmailProvider(IContext context, EmailProvider emailProviders, email_connector.proxies.EmailProvider mxEmailProvider) throws EmailConnectorException {
        for (com.mendix.datahub.connector.email.model.autoconfig.IncomingServer incomingServer : emailProviders.getIncomingServers()) {
            var mxIncomingServer = new IncomingServer(context);
            mxIncomingServer.setIncomingServer_EmailProvider(mxEmailProvider);
            mxIncomingServer.setHostName(incomingServer.getHostname());
            mxIncomingServer.setPort(incomingServer.getPort());
            mxIncomingServer.setSocketType(incomingServer.getSocketType());
            mxIncomingServer.setIncomingProtocol(getIncomingProxyProtocol(incomingServer.getType()));
        }
        for (com.mendix.datahub.connector.email.model.autoconfig.OutgoingServer outgoingServer : emailProviders.getOutgoingServers()) {
            var mxOutgoingServer = new OutgoingServer(context);
            mxOutgoingServer.setOutgoingServer_EmailProvider(mxEmailProvider);
            mxOutgoingServer.setHostName(outgoingServer.getHostname());
            mxOutgoingServer.setPort(outgoingServer.getPort());
            mxOutgoingServer.setSSL(outgoingServer.isUseSSL());
            mxOutgoingServer.setTLS(outgoingServer.isUseTLS());
            if (outgoingServer.getType().name().equals("SMTP"))
                mxOutgoingServer.setOutgoingProtocol(ENUM_OutgoingProtocol.SMTP);
        }
    }

    public static EmailMessage getEmailMessageFromTemplate(IContext context, EmailTemplate emailTemplate, Boolean queued) {
        var emailMessage = new EmailMessage(context);
        emailMessage.setTo(emailTemplate.getTo());
        emailMessage.setCC(emailTemplate.getCC());
        emailMessage.setBCC(emailTemplate.getBCC());
        emailMessage.setSubject(emailTemplate.getSubject());
        emailMessage.setContent(emailTemplate.getContent());
        emailMessage.setSentDate(emailTemplate.getSentDate());
        emailMessage.setPlainBody(emailTemplate.getPlainBody());
        emailMessage.setUseOnlyPlainText(emailTemplate.getUseOnlyPlainText());
        emailMessage.setReplyTo(emailTemplate.getReplyTo());
        emailMessage.setFromDisplayName(emailTemplate.getFromDisplayName());
        emailMessage.setFrom(emailTemplate.getFromAddress());
        if (Boolean.TRUE.equals(queued)) {
            emailMessage.setQueuedForSending(true);
            emailMessage.setStatus(ENUM_EmailStatus.QUEUED);
        }
        emailMessage.setEmailMessage_EmailTemplate(emailTemplate);
        emailMessage.setisSigned(emailTemplate.getSigned());
        emailMessage.setisEncrypted(emailTemplate.getEncrypted());
        return emailMessage;
    }
}
