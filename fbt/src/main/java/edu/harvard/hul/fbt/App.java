package edu.harvard.hul.fbt;

import edu.harvard.hul.fbt.cli.CLI;

public class App {
  public static void main( String[] args ) {
    CLI cli = new CLI();
    ControllerState state = new ControllerState();
    FitsXMLComparator comp = new FitsXMLComparator();
    Controller controller = new Controller( cli, state, comp );
    controller.setInput( args );
    controller.run();

    int exitCode = 1;

    try {

      exitCode = controller.getState().getExitCode();

    } catch ( Exception e ) {

      System.err.println( e.getMessage() );
      exitCode = ControllerState.SYSTEM_ERROR;

    } finally {
      System.out.println( String.format( "exitCode %d", exitCode ) );
      System.exit( exitCode );
    }
  }
}
