/**
 * Copyright 2010-2015 Axel Fontaine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.dbsupport.monetdb;

import org.flywaydb.core.internal.dbsupport.Delimiter;
import org.flywaydb.core.internal.dbsupport.SqlStatementBuilder;
import org.flywaydb.core.internal.util.StringUtils;

import java.util.regex.Pattern;

/**
 * SqlStatementBuilder supporting MonetDB-specific delimiter changes.
 */
public class MonetDBSqlStatementBuilder extends SqlStatementBuilder {
    /**
     * The keyword that indicates a change in delimiter.
     */
    private static final String DELIMITER_KEYWORD = "DELIMITER";

    /*private -> testing*/ boolean isInMultiLineCommentDirective = false;

    @Override
    public Delimiter extractNewDelimiterFromLine(String line) {
        if (line.toUpperCase().startsWith(DELIMITER_KEYWORD)) {
            return new Delimiter(line.substring(DELIMITER_KEYWORD.length()).trim(), false);
        }

        return null;
    }

    @Override
    protected Delimiter changeDelimiterIfNecessary(String line, Delimiter delimiter) {
        if (line.toUpperCase().startsWith(DELIMITER_KEYWORD)) {
            return new Delimiter(line.substring(DELIMITER_KEYWORD.length()).trim(), false);
        }

        return delimiter;
    }

    @Override
    public boolean isCommentDirective(String line) {
        // single-line comment directive
        if (line.matches("^" + Pattern.quote("/*!") + "\\d{5} .*" + Pattern.quote("*/") + "\\s*;?")) {
            return true;
        }
        // last line of multi-line comment directive
        if (isInMultiLineCommentDirective && line.matches(".*" + Pattern.quote("*/") + "\\s*;?")) {
            isInMultiLineCommentDirective = false;
            return true;
        }
        // start of multi-line comment directive
        if (line.matches("^" + Pattern.quote("/*!") + "\\d{5} .*")) {
            isInMultiLineCommentDirective = true;
            return true;
        }
        return isInMultiLineCommentDirective;
    }

    @Override
    public boolean isSingleLineComment(String line) {
        return line.startsWith("--");
    }

    @Override
    protected String removeEscapedQuotes(String token) {
        String noEscapedBackslashes = StringUtils.replaceAll(token, "\\\\", "");
        String noBackslashEscapes = StringUtils.replaceAll(StringUtils.replaceAll(noEscapedBackslashes, "\\'", ""), "\\\"", "");
        return StringUtils.replaceAll(noBackslashEscapes, "''", "");
    }

    @Override
    protected String extractAlternateOpenQuote(String token) {
        if (token.startsWith("\"")) {
            return "\"";
        }
        // to be a valid bitfield or hex literal the token must be at leas three characters in length
        // i.e. b'' otherwise token may be string literal ending in [space]b'
        if (token.startsWith("B'") && token.length() > 2) {
            return "B'";
        }
        if (token.startsWith("X'") && token.length() > 2) {
            return "X'";
        }
        return null;
    }

    @Override
    protected String computeAlternateCloseQuote(String openQuote) {
        if ("B'".equals(openQuote) || "X'".equals(openQuote)) {
            return "'";
        }
        return openQuote;
    }
}
