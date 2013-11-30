/*
 * Copyright 2013 Centro de Investigación en Tecnoloxías da Información (CITIUS),
 * University of Santiago de Compostela (USC).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.usc.citius.composit.cli.command;

import com.beust.jcommander.*;
import com.google.common.base.Stopwatch;
import es.usc.citius.composit.cli.CompositCli;
import es.usc.citius.composit.core.composition.optimization.BackwardMinimizationOptimizer;
import es.usc.citius.composit.core.composition.optimization.FunctionalDominanceOptimizer;
import es.usc.citius.composit.core.composition.search.ComposIT;
import es.usc.citius.composit.core.knowledge.Concept;
import es.usc.citius.composit.core.matcher.logic.LogicMatchType;
import es.usc.citius.composit.wsc08.data.WSCTest;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Parameters(commandDescription = "Search for an optimal composition")
public class CompositionCommand implements CliCommand {

    @Parameter(names = {"-d", "--dataset"}, description = "Select a WSC'08 dataset.", required = true)
    private WSCTest test;

    @Parameter(names = {"-i", "--inputs"}, description = "Select input concepts. If no inputs are specified, " +
            "then the default query inputs for the dataset will be used.")
    private Set<String> inputs;

    @Parameter(names = {"-o", "--outputs"}, description = "Select output concepts. If no outputs are specified, " +
            "then the default query outputs for the dataset will be used.")
    private Set<String> outputs;

    @Parameter(names = {"-ob", "--opt-backward"}, arity = 1, description = "Use backward optimization over the match network")
    private boolean backwardOptimization = true;

    @Parameter(names = {"-of", "--opt-fdominance"}, arity = 1, description = "Use functional dominance optimization over the match network")
    private boolean functionalDominance = true;

    @Parameter(names = {"-oeq", "--opt-node-eq"}, description = "Use node equivalence detection during search node expansion")
    private boolean nodeEquivalence = false;

    @Parameter(names = {"-b", "--benchmark-cycles"}, description = "Activate benchmark mode using N cycles")
    private int benchmarkCycles;

    @Parameter(names = {"-sm", "--semantic-match"}, description = "Select semantic match type allowed [ SUBSUMES | PLUGIN | EXACT ]")
    private LogicMatchType matchType = LogicMatchType.PLUGIN;

    public class LogicMatchTypeConverter implements IStringConverter<LogicMatchType> {

        public LogicMatchType convert(String value) {
            LogicMatchType convertedValue = LogicMatchType.valueOf(value);
            if(convertedValue == null) {
                throw new ParameterException("Value " + value + "can not be converted to LogicMatchType. " +
                        "Available values: " + LogicMatchType.values());
            }
            return convertedValue;
        }
    }

    @Override
    public void invoke(JCommander cli, CompositCli context) {
        // Print system information
        System.out.println("> Running composition on: " + this.test.toString());

        // Load dataset
        WSCTest.Dataset dataset = null;
        try {
            dataset = test.dataset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ComposIT<Concept, Boolean> composit = new ComposIT<Concept, Boolean>(dataset.getDefaultCompositionProblem());

        // Configure search
        if (backwardOptimization){
            composit.addOptimization(new BackwardMinimizationOptimizer<Concept, Boolean>());
        }
        if (functionalDominance){
            composit.addOptimization(new FunctionalDominanceOptimizer<Concept, Boolean>());
        }

        // Compute benchmark
        Stopwatch watch = Stopwatch.createUnstarted();
        long minMS = Long.MAX_VALUE;
        for(int i=0; i<benchmarkCycles; i++){
            System.out.println("[ComposIT Search] Starting search cycle " + (i+1));
            watch.start();
            composit.search(dataset.getRequest());
            long ms = watch.stop().elapsed(TimeUnit.MILLISECONDS);
            if (ms < minMS){
                minMS = ms;
            }
            watch.reset();
        }
        if (benchmarkCycles > 0){
            System.out.println("[Benchmark Result] " + benchmarkCycles + "-cycle benchmark completed. Best time: " + minMS + " ms.");
        }
    }

    @Override
    public String getCommandName() {
        return "compose";
    }

    public WSCTest getTest() {
        return test;
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    public boolean isBackwardOptimization() {
        return backwardOptimization;
    }

    public boolean isFunctionalDominance() {
        return functionalDominance;
    }

    public boolean isNodeEquivalence() {
        return nodeEquivalence;
    }

    public int getBenchmarkCycles() {
        return benchmarkCycles;
    }

    public LogicMatchType getMatchType() {
        return matchType;
    }
}