package com.datakasauti.engine.clean;

import com.datakasauti.dto.request.CleaningRuleDto;
import java.util.List;

/** Engine representation of a validated cleaning rule. */
public record CleaningRule(
        RuleType type,
        String column,
        List<String> columns,
        FillStrategy strategy,
        String value
) {
    public CleaningRule {
        columns = columns == null ? null : List.copyOf(columns);
    }

    public static CleaningRule from(CleaningRuleDto dto) {
        RuleType type = RuleType.from(dto.type());
        FillStrategy strategy = type == RuleType.FILL_MISSING && dto.strategy() != null
                ? FillStrategy.from(dto.strategy()) : null;
        return new CleaningRule(type, dto.column(), dto.columns(), strategy, dto.value());
    }
}
