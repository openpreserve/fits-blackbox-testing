package edu.harvard.hul.fbt;

import edu.harvard.hul.fbt.cli.CLI;

public class App {
  public static void main( String[] args ) {
    CLI cli = new CLI();
    Controller controller = new Controller( cli );
    controller.setInput( args );    
    int exitCode = controller.run();
    
    System.out.println( String.format( "exitCode %d", exitCode ) );
    System.exit( exitCode );
  }
}
