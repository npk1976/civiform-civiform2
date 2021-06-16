package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;

/** Represents a predicate that can be evaluated over {@link services.applicant.ApplicantData}. */
@AutoValue
public abstract class PredicateExpressionNode {

  @JsonCreator
  public static PredicateExpressionNode create(
      @JsonProperty("node") ConcretePredicateExpressionNode node) {
    return new AutoValue_PredicateExpressionNode(node);
  }

  @JsonProperty("node")
  public abstract ConcretePredicateExpressionNode node();

  @JsonIgnore
  @Memoized
  public PredicateExpressionNodeType getType() {
    return node().getType();
  }

  /** Get a leaf node if it exists, or throw if this is not a leaf node. */
  @JsonIgnore
  @Memoized
  public LeafOperationExpressionNode getLeafNode() {
    if (!(node() instanceof LeafOperationExpressionNode)) {
      throw new RuntimeException(
          String.format("Expected a LEAF node but received %s node", getType()));
    }
    return (LeafOperationExpressionNode) node();
  }

  /** Get an and node if it exists, or throw if this is not an and node. */
  @JsonIgnore
  @Memoized
  public AndNode getAndNode() {
    if (!(node() instanceof AndNode)) {
      throw new RuntimeException(
          String.format("Expected an AND node but received %s node", getType()));
    }
    return (AndNode) node();
  }

  /** Get an or node if it exists, or throw if this is not an or node. */
  @JsonIgnore
  @Memoized
  public OrNode getOrNode() {
    if (!(node() instanceof OrNode)) {
      throw new RuntimeException(
          String.format("Expected an OR node but received %s node", getType()));
    }
    return (OrNode) node();
  }

  @JsonIgnore
  @Memoized
  public ImmutableSet<Long> getQuestions() {
    ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
    getQuestions(builder, this);
    return builder.build();
  }

  private void getQuestions(
      ImmutableSet.Builder<Long> builder, PredicateExpressionNode currentNode) {
    switch (currentNode.getType()) {
      case AND:
        currentNode.getAndNode().children().stream()
            .flatMap(child -> child.getQuestions().stream())
            .forEach(builder::add);
        break;
      case OR:
        currentNode.getOrNode().children().stream()
            .flatMap(child -> child.getQuestions().stream())
            .forEach(builder::add);
        break;
      case LEAF_OPERATION:
        builder.add(currentNode.getLeafNode().questionId());
        break;
      default:
    }
  }
}
