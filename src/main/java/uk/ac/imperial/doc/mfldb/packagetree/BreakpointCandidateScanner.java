package uk.ac.imperial.doc.mfldb.packagetree;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link TreeScanner} implementation which scans for line numbers which can have breakpoints added.
 */
public class BreakpointCandidateScanner extends TreeScanner<Map<Long, BreakpointType>, SourcePositions> {

    private CompilationUnitTree compilationUnitTree;

    @Override
    public Map<Long, BreakpointType> reduce(Map<Long, BreakpointType> candidates1, Map<Long, BreakpointType> candidates2) {
        if (candidates1 != null && candidates2 != null) {
            candidates1.putAll(candidates2);
            return candidates1;
        } else {
            return candidates1 != null ? candidates1 : candidates2;
        }
    }

    @Override
    public Map<Long, BreakpointType> visitCompilationUnit(CompilationUnitTree compilationUnitTree, SourcePositions sourcePositions) {
        this.compilationUnitTree = compilationUnitTree;
        return super.visitCompilationUnit(compilationUnitTree, sourcePositions);
    }

    @Override
    public Map<Long, BreakpointType> visitClass(ClassTree classTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitClass(classTree, sourcePositions));
        result.put(startLine(classTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(classTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitMethod(MethodTree methodTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitMethod(methodTree, sourcePositions));
        result.put(startLine(methodTree, sourcePositions), BreakpointType.METHOD);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitVariable(VariableTree variableTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitVariable(variableTree, sourcePositions));
        result.put(startLine(variableTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitEmptyStatement(EmptyStatementTree emptyStatementTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitEmptyStatement(emptyStatementTree, sourcePositions));
        result.put(startLine(emptyStatementTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitBlock(BlockTree blockTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitBlock(blockTree, sourcePositions));
        result.put(startLine(blockTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(blockTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitDoWhileLoop(doWhileLoopTree, sourcePositions));
        result.put(startLine(doWhileLoopTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(doWhileLoopTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitWhileLoop(WhileLoopTree whileLoopTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitWhileLoop(whileLoopTree, sourcePositions));
        result.put(startLine(whileLoopTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(whileLoopTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitForLoop(ForLoopTree forLoopTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitForLoop(forLoopTree, sourcePositions));
        result.put(startLine(forLoopTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(forLoopTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = super.visitEnhancedForLoop(enhancedForLoopTree, sourcePositions);
        result.put(startLine(enhancedForLoopTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(enhancedForLoopTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitLabeledStatement(LabeledStatementTree labeledStatementTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = super.visitLabeledStatement(labeledStatementTree, sourcePositions);
        result.put(startLine(labeledStatementTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitSwitch(SwitchTree switchTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitSwitch(switchTree, sourcePositions));
        result.put(startLine(switchTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(switchTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitCase(CaseTree caseTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitCase(caseTree, sourcePositions));
        result.put(startLine(caseTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitSynchronized(SynchronizedTree synchronizedTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitSynchronized(synchronizedTree, sourcePositions));
        result.put(startLine(synchronizedTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(synchronizedTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitTry(TryTree tryTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitTry(tryTree, sourcePositions));
        result.put(startLine(tryTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(tryTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitCatch(CatchTree catchTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitCatch(catchTree, sourcePositions));
        result.put(startLine(catchTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(catchTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitExpressionStatement(ExpressionStatementTree expressionStatementTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitExpressionStatement(expressionStatementTree, sourcePositions));
        result.put(startLine(expressionStatementTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitBreak(BreakTree breakTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitBreak(breakTree, sourcePositions));
        result.put(startLine(breakTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitContinue(ContinueTree continueTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitContinue(continueTree, sourcePositions));
        result.put(startLine(continueTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitReturn(ReturnTree returnTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitReturn(returnTree, sourcePositions));
        result.put(startLine(returnTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitThrow(ThrowTree throwTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitThrow(throwTree, sourcePositions));
        result.put(startLine(throwTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitAssert(AssertTree assertTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitAssert(assertTree, sourcePositions));
        result.put(startLine(assertTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    @Override
    public Map<Long, BreakpointType> visitAnnotation(AnnotationTree annotationTree, SourcePositions sourcePositions) {
        Map<Long, BreakpointType> result = defaultIfNull(super.visitAnnotation(annotationTree, sourcePositions));
        result.put(startLine(annotationTree, sourcePositions), BreakpointType.LINE);
        result.put(endLine(annotationTree, sourcePositions), BreakpointType.LINE);
        return result;
    }

    /**
     * What do we want? Null-coalesce! When do we want it? NOW!
     */
    private Map<Long, BreakpointType> defaultIfNull(Map<Long, BreakpointType> result) {
        return result != null ? result : new HashMap<>();
    }

    private long startLine(Tree tree, SourcePositions sourcePositions) {
        long startPosition = sourcePositions.getStartPosition(compilationUnitTree, tree);
        return compilationUnitTree.getLineMap().getLineNumber(startPosition);
    }

    private long endLine(Tree tree, SourcePositions sourcePositions) {
        long endPosition = sourcePositions.getEndPosition(compilationUnitTree, tree);
        return compilationUnitTree.getLineMap().getLineNumber(endPosition);
    }
}
