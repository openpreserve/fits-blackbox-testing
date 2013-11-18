package edu.harvard.hul.fbt;

public class Anomaly {
  
  public static final String MISSING_TOOL = "tool.missing";

  public static final String MISSING_CANDIDATE = "file.candidate.missing";

  public static final String MISSING_SOURCE = "file.source.missing";

  private String mType;
  
  private String mFileName;
  
  private Object mData;
  
  public Anomaly(String type, String fileName) {
    mType = type;
    mFileName = fileName;
  }
  
  public String getType() {
    return mType;
  }
  
  public String getFileName() {
    return mFileName;
  }
  
  public void setData(Object o) {
    mData = o;
  }
  
  public Object getData() {
    return mData;
  }
}
