package edu.harvard.hul.fbt.cli;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class CLI {

  private CommandLineParser mParser;

  private Options mOptions;

  private String mComparisonKey;

  private String mSourceFolder;

  private String mCandidateFolder;

  public CLI() {
    mParser = new GnuParser();
    mOptions = new Options();
    mOptions.addOption(
        "k",
        "comparison-key",
        true,
        "Some comparison key that helps distinguish the versions that are being compared."
        );
    mOptions.addOption(
        "s",
        "source",
        true,
        "A folder that contains the output fits.xml files from the current stable version used to compare against."
        );
    mOptions.addOption(
        "c",
        "candidate",
        true,
        "A folder that contains the output fits.xml files from the merge candidate version."
        );
    mOptions.addOption(
        "h",
        "help",
        false,
        "Prints this message"
        );
  }

  public void parse( String... args ) throws ParseException {
    CommandLine cmd = mParser.parse( mOptions, args );

    if ( cmd.hasOption( 'h' ) ) {
      throw new ParseException( "HELP" );
    }
    
    if (cmd.hasOption( 's' )) {
      mSourceFolder = cmd.getOptionValue( 's' );
    } else {
      throw new ParseException( "Please provide a source folder containing fits.xml files from the current stable version" );
    }
    
    if (cmd.hasOption( 'c' )) {
      mCandidateFolder = cmd.getOptionValue( 'c' );
    } else {
      throw new ParseException( "Please provide a candidate folder containing fits.xml files from the merge-candidate version" );
    }
    
    if (cmd.hasOption( 'k' )) {
      mComparisonKey = cmd.getOptionValue( 'k' );
    } else {
      mComparisonKey = new SimpleDateFormat( "yyyyMMdd_HHmmss" ).format( new Date() );
    }
  }

  public CommandLineParser getParser() {
    return mParser;
  }

  public void setParser( CommandLineParser parser ) {
    mParser = parser;
  }

  public Options getOptions() {
    return mOptions;
  }

  public void setOptions( Options options ) {
    mOptions = options;
  }

  public String getComparisonKey() {
    return mComparisonKey;
  }

  public String getSourceFolderPath() {
    return mSourceFolder;
  }

  public String getCandidateFolderPath() {
    return mCandidateFolder;
  }
}
