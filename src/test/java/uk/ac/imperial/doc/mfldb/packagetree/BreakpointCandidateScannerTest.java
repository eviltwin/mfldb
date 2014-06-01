package uk.ac.imperial.doc.mfldb.packagetree;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.truth0.Truth.ASSERT;

/**
 * Tests for the {@link BreakpointCandidateScanner} class.
 */
public class BreakpointCandidateScannerTest {

    @SafeVarargs
    private static void verify(Map<Long, BreakpointType> map, Consumer<Map<Long, BreakpointType>>... expectations) {
        ASSERT.that(map).isNotNull();
        Map<Long, BreakpointType> copy = new HashMap<>(map);
        for (Consumer<Map<Long, BreakpointType>> expectation : expectations) {
            expectation.accept(copy);
        }
        copy.forEach((line, type) -> ASSERT.fail("Unexpected candidate %s breakpoint at line %s", type, line));
    }

    private static Consumer<Map<Long, BreakpointType>> line(long line, BreakpointType expected) {
        return map -> {
            BreakpointType actual = map.remove(line);
            ASSERT.withFailureMessage(String.format("Expected candidate %s breakpoint at line %d but found %s", expected, line, actual)).that(actual).is(expected);
        };
    }

    private static Map<Long, BreakpointType> getResultsForFile(String sourceFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        String fileURL = BreakpointCandidateScannerTest.class.getResource(sourceFile).getFile();
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(fileURL);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);

        Trees trees = Trees.instance(task);
        BreakpointCandidateScanner scanner = new BreakpointCandidateScanner();

        return scanner.scan(task.parse(), trees.getSourcePositions());
    }

    @Test
    public void helloWorld() throws IOException {
        // Given
        String file = "HelloWorld.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void emptyClass() throws IOException {
        // Given
        String file = "EmptyClass.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(5, BreakpointType.LINE)
        );
    }

    @Test
    public void simpleVariable() throws IOException {
        // Given
        String file = "SimpleVariable.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void emptyStatement() throws IOException {
        // Given
        String file = "EmptyStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void emptyBlock() throws IOException {
        // Given
        String file = "EmptyBlock.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void doWhileLoop() throws IOException {
        // Given
        String file = "DoWhileLoop.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE),
                line(11, BreakpointType.LINE)
        );
    }

    @Test
    public void whileLoop() throws IOException {
        // Given
        String file = "WhileLoop.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void forLoop() throws IOException {
        // Given
        String file = "ForLoop.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void enhancedForLoop() throws IOException {
        // Given
        String file = "EnhancedForLoop.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void labeledEmptyStatement() throws IOException {
        // Given
        String file = "LabeledEmptyStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void emptySwitch() throws IOException {
        // Given
        String file = "EmptySwitch.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void switchCases() throws IOException {
        // Given
        String file = "SwitchCases.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE),
                line(11, BreakpointType.LINE),
                line(12, BreakpointType.LINE),
                line(13, BreakpointType.LINE)
        );
    }

    @Test
    public void EmptySynchronized() throws IOException {
        // Given
        String file = "EmptySynchronized.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void tryCatchFinally() throws IOException {
        // Given
        String file = "TryCatchFinally.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE),
                line(11, BreakpointType.LINE),
                line(12, BreakpointType.LINE),
                line(13, BreakpointType.LINE),
                line(14, BreakpointType.LINE),
                line(15, BreakpointType.LINE),
                line(16, BreakpointType.LINE)
        );
    }

    @Test
    public void conditionalExpression() throws IOException {
        // Given
        String file = "ConditionalExpression.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void IfStatement() throws IOException {
        // Given
        String file = "IfStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE),
                line(10, BreakpointType.LINE)
        );
    }

    @Test
    public void expressionStatement() throws IOException {
        // Given
        String file = "ExpressionStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void breakStatement() throws IOException {
        // Given
        String file = "BreakStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void continueStatement() throws IOException {
        // Given
        String file = "ContinueStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void returnStatement() throws IOException {
        // Given
        String file = "ReturnStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void throwStatement() throws IOException {
        // Given
        String file = "ThrowStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void assertStatement() throws IOException {
        // Given
        String file = "AssertStatement.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void newClass() throws IOException {
        // Given
        String file = "NewClass.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void newArray() throws IOException {
        // Given
        String file = "NewArray.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void lambdaExpression() throws IOException {
        // Given
        String file = "LambdaExpression.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(6, BreakpointType.METHOD),
                line(7, BreakpointType.LINE),
                line(8, BreakpointType.LINE),
                line(9, BreakpointType.LINE)
        );
    }

    @Test
    public void annotations() throws IOException {
        // Given
        String file = "Annotations.java";

        // When
        Map<Long, BreakpointType> map = getResultsForFile(file);

        // Then
        verify(map,
                line(4, BreakpointType.LINE),
                line(5, BreakpointType.LINE),
                line(7, BreakpointType.METHOD),
                line(8, BreakpointType.LINE),
                line(10, BreakpointType.LINE),
                line(11, BreakpointType.LINE),
                line(12, BreakpointType.LINE),
                line(13, BreakpointType.LINE)
        );
    }
}
