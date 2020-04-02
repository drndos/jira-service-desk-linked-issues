package sk.drndos.jira.sdlinks;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SdLinksGroupsModel {

  private final String groupName;
  private final List<SdLinksModel> links;

}
