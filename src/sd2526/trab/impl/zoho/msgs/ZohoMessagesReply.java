package sd2526.trab.impl.zoho.msgs;

import java.util.List;

public record ZohoMessagesReply(ZohoStatus status, List<ZohoMessage> data) {
}