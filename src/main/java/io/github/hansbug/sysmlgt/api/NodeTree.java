package io.github.hansbug.sysmlgt.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
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

    public static TreeNode from(PilotParser.ParseResult result) {
        return map(result.rootNode());
    }

    private static TreeNode map(INode node) {
        List<TreeNode> children = new ArrayList<>();
        if (node instanceof ICompositeNode composite) {
            for (INode child : composite.getChildren()) children.add(map(child));
        }
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
