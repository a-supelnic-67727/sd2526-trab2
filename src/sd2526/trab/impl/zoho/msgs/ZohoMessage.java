package sd2526.trab.impl.zoho.msgs;

public record ZohoMessage(
        String messageId,
        String fromAddress,
        String toAddress,
        String subject,
        String content,
        String receivedTime) {
}