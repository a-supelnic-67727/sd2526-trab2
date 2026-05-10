package sd2526.trab.impl.zoho.msgs;

import java.util.List;

public record ZohoFoldersReply(ZohoStatus status, List<ZohoFolder> data) {
}