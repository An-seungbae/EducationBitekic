package email_connector.implementation;

import com.mendix.datahub.connector.email.model.FetchStrategy;
import com.mendix.datahub.connector.email.model.Protocol;
import com.mendix.datahub.connector.email.utils.EmailConnectorException;
import com.mendix.datahub.connector.email.utils.Error;
import email_connector.proxies.ENUM_AttachmentPosition;
import email_connector.proxies.ENUM_IncomingProtocol;
import email_connector.proxies.ENUM_MessageHandling;

public class Commons {

    private Commons() {
    }

    public static Protocol getProtocol(email_connector.proxies.ENUM_IncomingProtocol protocol) throws EmailConnectorException {
        if (protocol == null)
            throw new EmailConnectorException(Error.EMPTY_PROTOCOL.getMessage());
        else if (protocol.name().equals("IMAPS"))
            return Protocol.IMAPS;
        else if (protocol.name().equals("IMAP"))
            return Protocol.IMAP;
        else if (protocol.name().equals("POP3"))
            return Protocol.POP3;
        else
            return Protocol.POP3S;
    }

    public static ENUM_IncomingProtocol getIncomingProxyProtocol(Protocol protocol) throws EmailConnectorException {
        if (protocol == null)
            throw new EmailConnectorException(Error.EMPTY_PROTOCOL.getMessage());
        else if (protocol.name().equals("IMAPS"))
            return ENUM_IncomingProtocol.IMAPS;
        else if (protocol.name().equals("IMAP"))
            return ENUM_IncomingProtocol.IMAP;
        else if (protocol.name().equals("POP3"))
            return ENUM_IncomingProtocol.POP3;
        else
            return ENUM_IncomingProtocol.POP3S;
    }

    public static ENUM_AttachmentPosition getPosition(String attachmentPosition) throws EmailConnectorException {
        if (attachmentPosition == null || attachmentPosition.isEmpty())
            throw new EmailConnectorException(Error.EMPTY_ATTACHMENT_POSITION.getMessage());
        else if (attachmentPosition.equalsIgnoreCase("attachment"))
            return ENUM_AttachmentPosition.Attachment;
        else
            return ENUM_AttachmentPosition.Inline;
    }

    public static String getHandling(ENUM_MessageHandling messageHandling) throws EmailConnectorException {
        if (messageHandling == null)
            throw new EmailConnectorException(Error.EMPTY_MESSAGE_HANDLING.getMessage());
        else if (messageHandling.equals(ENUM_MessageHandling.DeleteMessage))
            return "DeleteMessage";
        else if (messageHandling.equals(ENUM_MessageHandling.MoveMessage))
            return "MoveMessage";
        else
            return "NoHandling";
    }

    public static FetchStrategy getFetchStrategy(email_connector.proxies.ENUM_FetchStrategy fetchStrategy) throws EmailConnectorException {
        if (fetchStrategy == null)
            throw new EmailConnectorException(Error.EMPTY_FETCH_STRATEGY.getMessage());
        else if (fetchStrategy.name().equalsIgnoreCase("Latest"))
            return FetchStrategy.LATEST;
        else
            return FetchStrategy.OLDEST;
    }
}
