package org.gr8conf.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.Message
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.ExpressionStatement

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class PreconditionASTTransformation implements ASTTransformation {

    SourceUnit sourceUnit

    @Override void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (astNodes?.size() != 2 || !sourceUnit) return

        this.sourceUnit = sourceUnit

        def annotation = astNodes[0]
        def annotatedNode = astNodes[1]

        injectPrecondition(annotatedNode, annotation)
    }

    def injectPrecondition(MethodNode methodNode, AnnotationNode annotationNode)  {

        ClosureExpression closureExpression = extractClosureExpression(annotationNode)
        BooleanExpression booleanExpression = extractBooleanExpression(closureExpression)
        if (!booleanExpression)  {
            sourceUnit.errorCollector.addError Message.create("@Precondition does not contain a boolean expression", sourceUnit), false
            return
        }

        AssertStatement assertStatement = createAssertStatement(booleanExpression)

        addAssertStatementToMethodCodeBlock(methodNode, assertStatement)
    }

    def extractClosureExpression(AnnotationNode annotationNode) {
        return annotationNode.getMember('value')
    }

    def extractBooleanExpression(ClosureExpression closureExpression)  {
        for (Statement stmt : closureExpression.getCode().getStatements())  {
            if (stmt instanceof ExpressionStatement)  {
                BooleanExpression result = new BooleanExpression(stmt.getExpression())
                result.setSourcePosition closureExpression
                result.setSynthetic true
                return result
            }
        }
        return null
    }

    def createAssertStatement(BooleanExpression booleanExpression)  {
        AssertStatement assertStatement = new AssertStatement(booleanExpression)
        assertStatement.setSourcePosition booleanExpression

        return assertStatement
    }

    def addAssertStatementToMethodCodeBlock(MethodNode methodNode, AssertStatement assertStatement) {
        methodNode.getCode().getStatements().add(0, assertStatement)
    }
}
