package edu.harvard.hul.fdc;

public class ControllerState {

  public static final int OK = 0;

  public static final int SYSTEM_ERROR = 1;

  public static final int FILE_MISSING_SOURCE = 2;

  public static final int FILE_MISSING_CANDIDATE = 3;

  public static final int FILE_INVALID_OUTPUT_SOURCE = 4;

  public static final int FILE_INVALID_OUTPUT_CANDIDATE = 5;

  public static final int TOOL_MISSING_OUTPUT = 6;

  public static final int TOOL_VERSION_MISMATCH = 7;

  public static final int TOOL_VALUE_MISMATCH = 8;

  public static final int CONFLICT_RESOLUTION = 9;

  public static final int CONFLICT_INTRODUCTION = 10;

  public static final int MULTIPLE_PROBLEMS = 42;

  private int mState = OK;

  private int[] mStates;

  public ControllerState() {
    mStates = new int[] { OK, SYSTEM_ERROR, FILE_MISSING_SOURCE, FILE_MISSING_CANDIDATE, FILE_INVALID_OUTPUT_SOURCE,
        FILE_INVALID_OUTPUT_CANDIDATE, TOOL_MISSING_OUTPUT, TOOL_VERSION_MISMATCH, TOOL_VALUE_MISMATCH,
        CONFLICT_RESOLUTION, CONFLICT_INTRODUCTION, MULTIPLE_PROBLEMS };
  }

  public void assignState(int state) {
    if (!isValidState(state)) {
      mState = SYSTEM_ERROR;
    } else if (state == SYSTEM_ERROR) {
      mState = SYSTEM_ERROR;
    } else if (state == OK) {
      mState = (mState == OK) ? state : mState;
    } else {
      mState = (mState == OK || mState == state) ? state : MULTIPLE_PROBLEMS;
    }

  }

  public int getExitCode() {
    return mState;
  }

  public boolean isValidState(int state) {
    boolean exists = false;
    for (int s : mStates) {
      if (s == state) {
        exists = true;
        break;
      }
    }
    return exists;
  }
}
