package edu.harvard.hul.fbt;

import java.util.List;

import org.dom4j.Element;

public abstract class DiffResolver {

  public abstract void resolve(String fileName, Element source, Element candidate );
  
  public abstract List<Report> report();
}
