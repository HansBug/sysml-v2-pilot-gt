package io.github.hansbug.sysmlgt.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;

/** JSON-friendly view of the official Xtext node model. */
public final class NodeTree {
    private NodeTree() {}

    /** Controls whether redundant same-range composite nodes are retained. */
    public enum Mode {
        MINIMAL,
        FULL;

        public static Mode parse(String value) {
            if (value == null || value.isBlank()) return MINIMAL;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("unknown tree mode '" + value
                        + "' (expected minimal or full)", error);
            }
        }
    }

    /** Build the compact, LLM-friendly projection used by default. */
    public static TreeNode from(PilotParser.ParseResult result) {
        return from(result, Mode.MINIMAL);
    }

    /** Build either the compact projection or the unmodified official tree. */
    public static TreeNode from(PilotParser.ParseResult result, Mode mode) {
        if (result == null) throw new NullPointerException("result");
        if (mode == null) throw new NullPointerException("mode");
        return mode == Mode.FULL ? mapFull(result.rootNode()) : mapMinimal(result.rootNode(), true);
    }

    /** Build the complete official Xtext NodeModel projection. */
    public static TreeNode fullFrom(PilotParser.ParseResult result) {
        return from(result, Mode.FULL);
    }

    private static TreeNode mapFull(INode node) {
        List<TreeNode> children = new ArrayList<>();
        if (node instanceof ICompositeNode composite) {
            for (INode child : composite.getChildren()) children.add(mapFull(child));
        }
        return node(node, children);
    }

    /**
     * Collapse a same-range chain while retaining every range-changing child
     * and every leaf. The entry node remains as a stable root anchor.
     */
    private static TreeNode mapMinimal(INode node, boolean root) {
        List<TreeNode> children = new ArrayList<>();
        if (node instanceof ICompositeNode composite) {
            for (INode child : composite.getChildren()) {
                TreeNode mapped = mapMinimal(child, false);
                if (mapped != null) children.add(mapped);
            }
        }
        boolean leaf = node instanceof ILeafNode;
        boolean hasRangeChangingChild = children.stream().anyMatch(child ->
                child.offset() != node.getTotalOffset() || child.endOffset() != node.getTotalEndOffset());
        if (!root && !leaf && !hasRangeChangingChild && children.size() == 1) {
            return children.get(0);
        }
        // Multiple same-range children cannot be promoted into one TreeNode
        // without inventing a synthetic node, so retain this official parent.
        return node(node, children);
    }

    private static TreeNode node(INode node, List<TreeNode> children) {
        String kind = node instanceof ILeafNode leaf && leaf.isHidden() ? "HiddenLeafNode"
                : node instanceof ICompositeNode ? "CompositeNode" : "LeafNode";
        return new TreeNode(kind, grammarName(node.getGrammarElement()), node.getTotalOffset(),
                node.getTotalEndOffset(), node.getText(), children.isEmpty() ? null : children);
    }

    private static String grammarName(EObject element) {
        if (element == null) return null;
        if (element instanceof RuleCall call) {
            AbstractRule rule = call.getRule();
            return "RuleCall(" + (rule == null ? "?" : rule.getName()) + ")";
        }
        if (element instanceof Keyword keyword) return "Keyword(" + keyword.getValue() + ")";
        if (element instanceof Action action) return "Action(" + action.getFeature() + " " + action.getOperator() + ")";
        return element.eClass().getName();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record TreeNode(String kind, String grammarElement, int offset, int endOffset,
                           String text, List<TreeNode> children) {}
}
