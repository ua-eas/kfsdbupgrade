package ua.utility.kfsdbupgrade.mdoc;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;

import org.w3c.dom.Node;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public enum PathFunction implements Function<Node, ImmutableList<Node>> {
  INSTANCE;

  @Override
  public ImmutableList<Node> apply(Node input) {
    if (input.getParentNode() == null) {
      return of(input);
    } else {
      return copyOf(concat(apply(input.getParentNode()), asList(input)));
    }
  }

}
