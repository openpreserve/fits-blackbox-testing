package edu.harvard.hul.fdc;

import edu.harvard.hul.fdc.cli.CLI;

public class App {
  public static void main( String[] args ) {
    CLI cli = new CLI();
    ControllerState state = new ControllerState();
    FitsXMLComparator comp = new FitsXMLComparator();
    LogWriter log = new LogWriter();
    Controller controller = new Controller( cli, state, comp, log );
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
