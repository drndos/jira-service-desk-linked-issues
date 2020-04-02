package sk.drndos.jira.sdlinks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SdLinksModel {

  private final String linkName;
  private final String issueKey;
  private final String issueSummary;
  private final String issueTypeIcon;
  private final String issueTypeName;
  private final String issueTypeDescription;
  private final String priorityIcon;
  private final String priorityDescription;
  private final String statusDescription;
  private final String statusName;
  private final String statusStyle;
  private final boolean resolved;

}