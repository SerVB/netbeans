/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.javascript2.editor.hints;

import com.oracle.js.parser.Token;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.ClassNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.JoinPredecessorExpression;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.ParameterNode;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.js.parser.ir.Statement;
import com.oracle.js.parser.ir.TernaryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.JsPreferences;
import org.netbeans.modules.javascript2.editor.JsVersion;
import static org.netbeans.modules.javascript2.editor.hints.EcmaLevelRule.refresh;
import org.netbeans.modules.javascript2.lexer.api.JsTokenId;
import org.netbeans.modules.javascript2.lexer.api.LexUtilities;
import org.netbeans.modules.javascript2.model.api.ModelUtils;
import org.netbeans.modules.javascript2.model.spi.PathNodeVisitor;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

public class Ecma6Rule extends EcmaLevelRule {

    private static final List<JsTokenId> ECMA6LIST = Arrays.asList(
            JsTokenId.TEMPLATE_BEGIN,
            JsTokenId.TEMPLATE,
            JsTokenId.TEMPLATE_END,
            JsTokenId.TEMPLATE_EXP_BEGIN,
            JsTokenId.TEMPLATE_EXP_END,
            JsTokenId.KEYWORD_EXPORT,
            JsTokenId.KEYWORD_IMPORT
            );

    @Override
    void computeHints(JsHintsProvider.JsRuleContext context, List<Hint> hints, int offset, HintsProvider.HintsManager manager) throws BadLocationException {
        if (JsPreferences.isPreECMAScript6(FileOwnerQuery.getOwner(context.getJsParserResult().getSnapshot().getSource().getFileObject()))) {
            Snapshot snapshot = context.getJsParserResult().getSnapshot();
            TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(snapshot, context.lexOffset);
            OffsetRange returnOffsetRange;
            if (ts != null) {
                while (ts.moveNext()) {
                    org.netbeans.api.lexer.Token<? extends JsTokenId> token = LexUtilities.findNextIncluding(ts, ECMA6LIST);
                    if (token != null && token.length() >= 1 && ECMA6LIST.contains(token.id())) {
                        returnOffsetRange = new OffsetRange(ts.offset(), ts.offset() + token.length());
                        addHint(context, hints, returnOffsetRange);
                    }
                }
            }
            
            Ecma6Visitor visitor = new Ecma6Visitor();
            visitor.process(context, hints);
        }
    }

    private void addHint(JsHintsProvider.JsRuleContext context, List<Hint> hints, OffsetRange range) {
        addDocumenHint(context, hints, ModelUtils.documentOffsetRange(context.getJsParserResult(),
                range.getStart(), range.getEnd()));
    }

    private void addDocumenHint(JsHintsProvider.JsRuleContext context, List<Hint> hints, OffsetRange range) {
        hints.add(new Hint(this, Bundle.Ecma6Desc(),
                context.getJsParserResult().getSnapshot().getSource().getFileObject(),
                range, Collections.singletonList(
                        new Ecma6Rule.SwitchToEcma6Fix(context.getJsParserResult().getSnapshot())), 600));
    }

    @Override
    public Set<?> getKinds() {
        return Collections.singleton(JsAstRule.JS_OTHER_HINTS);
    }

    @Override
    public String getId() {
        return "ecma6.hint";
    }

    @NbBundle.Messages("Ecma6Desc=ECMA6 feature used in pre-ECMA6 source")
    @Override
    public String getDescription() {
        return Bundle.Ecma6Desc();
    }

    @NbBundle.Messages("Ecma6DisplayName=ECMA6 feature used")
    @Override
    public String getDisplayName() {
        return Bundle.Ecma6DisplayName();
    }

    private class Ecma6Visitor extends PathNodeVisitor {

        private List<Hint> hints;

        private JsHintsProvider.JsRuleContext context;

        public void process(JsHintsProvider.JsRuleContext context, List<Hint> hints) {
            this.hints = hints;
            this.context = context;
            FunctionNode root = context.getJsParserResult().getRoot();
            if (root != null) {
                context.getJsParserResult().getRoot().accept(this);
            }
        }
        
        private boolean isDefaultParameter(FunctionNode functionNode, IdentNode param) {
            Block body = functionNode.getBody();
            if (body == null) {
                return false;
            }
            for (Statement st : body.getStatements()) {
                if (st instanceof ExpressionStatement) {
                    continue;
                }
                if (!(st instanceof VarNode)) {
                    return false;
                }
                VarNode vn = (VarNode)st;
                if (vn.getName() != param) {
                    continue;
                }
                
                
                // structural check, see Parser.addDefaultParameter
                if (!(vn.getInit() instanceof TernaryNode)) {
                    return false;
                }
                TernaryNode tn = (TernaryNode)vn.getInit();
                if (!(tn.getTest() instanceof BinaryNode)) {
                    return false;
                }
                BinaryNode bn = (BinaryNode)tn.getTest();
                if (!(
                    (bn.getLhs() instanceof ParameterNode) &&
                    (bn.getRhs() instanceof UnaryNode) &&
                    (bn.tokenType() == TokenType.EQ_STRICT) &&
                    (bn.getRhs().tokenType() == TokenType.VOID))) {
                    return false;
                }
                if (!((tn.getTrueExpression() instanceof JoinPredecessorExpression) && (tn.getFalseExpression() instanceof JoinPredecessorExpression))) {
                    return false;
                }
                Expression e = ((JoinPredecessorExpression)tn.getFalseExpression()).getExpression();
                if (e != bn.getLhs()) {
                    return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean enterFunctionNode(FunctionNode functionNode) {
            if (functionNode.getKind() == FunctionNode.Kind.ARROW) {
                addHint(context, hints, new OffsetRange(Token.descPosition(functionNode.getFirstToken()), functionNode.getFinish()));
            }
            if (functionNode.getKind() == FunctionNode.Kind.NORMAL) {
                for (IdentNode param : functionNode.getParameters()) {
                    if (isDefaultParameter(functionNode, param) || param.isRestParameter()) {
                        addHint(context, hints, new OffsetRange(param.getStart(), param.getFinish()));
                    }
                }
            }
            return super.enterFunctionNode(functionNode);
        }

        @Override
        public boolean enterClassNode(ClassNode classNode) {
            int headEnd = findToken(classNode.getStart(), JsTokenId.BRACKET_LEFT_CURLY);
            addHint(context, hints, new OffsetRange(classNode.getStart(), headEnd));
            return super.enterClassNode(classNode);
        }

        @Override
        public boolean enterVarNode(VarNode varNode) {
            long token = varNode.getToken();
            TokenType type = Token.descType(token);

            if (TokenType.LET == type || TokenType.CONST == type) {
                addHint(context, hints, new OffsetRange(varNode.getStart(), varNode.getFinish()));
            }

            return super.enterVarNode(varNode);
        }

        @Override
        public boolean enterLiteralNode(LiteralNode literalNode) {
            if (literalNode instanceof LiteralNode.ArrayLiteralNode && ((LiteralNode.ArrayLiteralNode) literalNode).hasSpread()) {
                addHint(context, hints, new OffsetRange(literalNode.getStart(), literalNode.getFinish()));
            }

            return super.enterLiteralNode(literalNode);
        }

        @Override
        public boolean enterPropertyNode(PropertyNode propertyNode) {
            if (propertyNode.isComputed()) {
                addHint(context, hints, new OffsetRange(propertyNode.getStart(), propertyNode.getFinish()));
            }
            return super.enterPropertyNode(propertyNode);
        }

        @Override
        public boolean enterUnaryNode(UnaryNode unaryNode) {
            if (unaryNode.isTokenType(TokenType.AWAIT) || unaryNode.isTokenType(TokenType.YIELD)) {
                long token = unaryNode.getToken();
                int position = Token.descPosition(token);
                addHint(context, hints, new OffsetRange(position, position + Token.descLength(token)));
            }
            return super.enterUnaryNode(unaryNode);
        }

        private int findToken(int offset, JsTokenId tokenId) {
            int fileOffset = context.parserResult.getSnapshot().getOriginalOffset(offset);
            if (fileOffset >= 0) {
                TokenSequence<? extends JsTokenId> ts = LexUtilities.getPositionedSequence(
                        context.parserResult.getSnapshot(), offset, JsTokenId.javascriptLanguage());
                if (ts != null) {
                    while (ts.moveNext()) {
                        org.netbeans.api.lexer.Token<? extends JsTokenId> next = LexUtilities.findNextNonWsNonComment(ts);
                        if (next != null && next.id() == tokenId) {
                            org.netbeans.api.lexer.Token<? extends JsTokenId> prev = LexUtilities.findPrevious(ts, Arrays.asList(JsTokenId.WHITESPACE, JsTokenId.EOL, JsTokenId.LINE_COMMENT, JsTokenId.BLOCK_COMMENT, JsTokenId.DOC_COMMENT, tokenId));
                            if (prev != null) {
                                return ts.offset() + prev.length();
                            }
                        }
                    }

                }
            }
            return -1;
        }
    }
    
    private static final class SwitchToEcma6Fix implements HintFix {

        private final FileObject fo;

        public SwitchToEcma6Fix(Snapshot snapshot) {
            this.fo = snapshot.getSource().getFileObject();
        }

        @NbBundle.Messages("MSG_SwitchToEcma6=Switch project to ECMA6")
        @Override
        public String getDescription() {
            return Bundle.MSG_SwitchToEcma6();
        }

        @Override
        public void implement() throws Exception {
            if (fo == null) {
                return;
            }

            Project p = FileOwnerQuery.getOwner(fo);
            if (p != null) {
                JsPreferences.putECMAScriptVersion(p, JsVersion.ECMA6);
            }

            refresh(fo);
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }
    }
}
