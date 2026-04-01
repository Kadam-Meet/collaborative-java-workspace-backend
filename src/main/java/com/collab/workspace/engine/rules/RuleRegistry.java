package com.collab.workspace.engine.rules;

import com.collab.workspace.analysis.rules.AnalysisRule;
import com.collab.workspace.analysis.rules.core.AssignmentInConditionRule;
import com.collab.workspace.analysis.rules.core.ConsoleLoggingRule;
import com.collab.workspace.analysis.rules.optimization.LoopOptimizationRule;
import com.collab.workspace.analysis.rules.optimization.StringConcatRule;
import com.collab.workspace.analysis.rules.runtime.DivisionByZeroRule;
import com.collab.workspace.analysis.rules.runtime.InfiniteLoopRule;
import com.collab.workspace.analysis.rules.runtime.NullPointerRule;
import com.collab.workspace.analysis.rules.smell.DeepNestingRule;
import com.collab.workspace.analysis.rules.smell.EmptyCatchRule;
import com.collab.workspace.analysis.rules.smell.GenericCatchRule;
import com.collab.workspace.analysis.rules.smell.ResourceLeakRule;
import com.collab.workspace.analysis.rules.type.RawTypeRule;
import com.collab.workspace.analysis.rules.type.UnsafeCastRule;

import java.util.List;

public class RuleRegistry {

	private final List<AnalysisRule> rules = List.of(
		new ConsoleLoggingRule(),
		new AssignmentInConditionRule(),
		new NullPointerRule(),
		new DivisionByZeroRule(),
		new InfiniteLoopRule(),
		new RawTypeRule(),
		new UnsafeCastRule(),
		new DeepNestingRule(),
		new GenericCatchRule(),
		new EmptyCatchRule(),
		new ResourceLeakRule(),
		new StringConcatRule(),
		new LoopOptimizationRule()
	);

	public List<AnalysisRule> getAllRules() {
		return rules;
	}

	public List<String> describeRules() {
		return rules.stream()
			.map(rule -> rule.getCategory() + ":" + rule.getRuleName())
			.toList();
	}
}
