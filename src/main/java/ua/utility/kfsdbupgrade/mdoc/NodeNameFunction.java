package ua.utility.kfsdbupgrade.mdoc;

import org.w3c.dom.Node;

import com.google.common.base.Function;

public enum NodeNameFunction implements Function<Node, String> {
  INSTANCE;

  @Override
  public String apply(Node input) {
    return input.getNodeName();
  }

}