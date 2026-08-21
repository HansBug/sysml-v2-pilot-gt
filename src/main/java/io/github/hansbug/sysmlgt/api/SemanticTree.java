package io.github.hansbug.sysmlgt.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

/** JSON-friendly projection of the official EMF semantic AST. */
public final class SemanticTree {
    private SemanticTree() {}

    public static SemanticNode from(PilotParser.ParseResult result) {
        return from(result.semanticRoot());
    }

    public static SemanticNode from(EObject root) {
        return root == null ? null : map(root, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    public static String typeName(EObject object) {
        if (object == null || object.eClass() == null) return null;
        String name = object.eClass().getName();
        if (name != null && !name.isBlank()) return name;
        return object.eClass().getInstanceClassName();
    }

    private static SemanticNode map(EObject object, java.util.Set<EObject> active) {
        if (!active.add(object)) {
            return new SemanticNode(typeName(object), uri(object), Map.of(), Map.of(), List.of());
        }
        try {
            Map<String, Object> attributes = new LinkedHashMap<>();
            Map<String, Object> references = new LinkedHashMap<>();
            List<Child> children = new ArrayList<>();
            for (EStructuralFeature feature : object.eClass().getEAllStructuralFeatures()) {
                if (feature.isDerived()) continue;
                Object value;
                try {
                    value = object.eGet(feature, false);
                } catch (RuntimeException ignored) {
                    // Some optional derived delegates are unavailable outside
                    // the full Eclipse runtime; omit only that feature.
                    continue;
                }
                if (empty(value)) continue;
                if (feature instanceof EReference reference && reference.isContainment()) {
                    addChildren(children, feature.getName(), value, active);
                } else if (feature instanceof EAttribute) {
                    attributes.put(feature.getName(), jsonValue(value));
                } else if (feature instanceof EReference) {
                    references.put(feature.getName(), referenceValue(value));
                }
            }
            return new SemanticNode(typeName(object), uri(object), attributes, references, children);
        } finally {
            active.remove(object);
        }
    }

    private static void addChildren(List<Child> children, String feature, Object value,
                                    java.util.Set<EObject> active) {
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof EObject child) children.add(new Child(feature, map(child, active)));
            }
        } else if (value instanceof EObject child) {
            children.add(new Child(feature, map(child, active)));
        }
    }

    private static Object referenceValue(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : iterable) result.add(item instanceof EObject e ? reference(e) : jsonValue(item));
            return result;
        }
        return value instanceof EObject e ? reference(e) : jsonValue(value);
    }

    private static Reference reference(EObject object) {
        return new Reference(typeName(object), uri(object), object.eIsProxy());
    }

    private static Object jsonValue(Object value) {
        if (value instanceof Enumerator enumerator) return enumerator.getLiteral();
        if (value instanceof Iterable<?> iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : iterable) result.add(jsonValue(item));
            return result;
        }
        return value;
    }

    private static boolean empty(Object value) {
        return value == null || (value instanceof Iterable<?> iterable && !iterable.iterator().hasNext());
    }

    private static String uri(EObject object) {
        try {
            var uri = EcoreUtil.getURI(object);
            return uri == null ? null : uri.toString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record SemanticNode(String type, String uri, Map<String, Object> attributes,
                               Map<String, Object> references, List<Child> children) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Child(String feature, SemanticNode node) {}

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Reference(String type, String uri, boolean proxy) {}
}
