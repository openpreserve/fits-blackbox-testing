package edu.harvard.hul.fbt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComparisonResult {

  private Set<Integer> mStatusCodes;
  
  private List<String> mLogs;
  
  public ComparisonResult() {
    mStatusCodes = new HashSet<Integer>();
    mLogs = new ArrayList<String>();
  }
  
  public ComparisonResult(int statusCode) {
    this();
    mStatusCodes.add(statusCode);
  }
  
  public ComparisonResult(int statusCode, List<String> logs) {
    this();
    mStatusCodes.add(statusCode);
    mLogs.addAll(logs);
  }
  
  public ComparisonResult(Set<Integer> statusCodes, List<String> logs) {
    mStatusCodes = statusCodes;
    mLogs = logs;
  }
  
  public Set<Integer> getStatusCodes() {
    return mStatusCodes;
  }
  
  public List<String> getLogs() {
    return mLogs;
  }
  
  public void mergeResults(ComparisonResult toMerge) {
    mStatusCodes.addAll(toMerge.getStatusCodes());
    mLogs.addAll(toMerge.getLogs());
  }
}
