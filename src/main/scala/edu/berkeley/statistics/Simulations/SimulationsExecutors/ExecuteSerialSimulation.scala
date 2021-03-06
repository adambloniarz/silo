/**
 * Copyright 2015 Adam Bloniarz, Christopher Wu, Ameet Talwalkar
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.berkeley.statistics.Simulations.SimulationsExecutors

import edu.berkeley.statistics.SerialForest.{RandomForest, RandomForestParameters, Tree, TreeParameters}
import edu.berkeley.statistics.Simulations.DataGenerators.{FourierBasisGaussianProcessGenerator, FourierBasisGaussianProcessFunction, Friedman1Generator}
import edu.berkeley.statistics.Simulations.EvaluationMetrics
import scala.math.floor

object ExecuteSerialSimulation {
  def main(args: Array[String]) = {

    val simulationName = args(0)
    val nTrain: Int = args(1).toInt
    val nTest: Int = args(2).toInt

    // Generate the data
    val (dataGenerator, forestParameters) = simulationName match {
      case "Friedman1" => {
        val numNonsenseDimensions: Int = if (args.size > 3) args(3).toInt else 5
        (Friedman1Generator(numNonsenseDimensions),
            RandomForestParameters(100, true, TreeParameters(3, 10)))
      }
      case "GaussianProcess" => {
        val numActiveDimensions = if (args.size > 3) args(3).toInt else 20
        val numInactiveDimensions = if (args.size > 4) args(4).toInt else 20
        val numBasisFunctions = if (args.length > 5) args(5).toInt else numActiveDimensions * 500
        val function = FourierBasisGaussianProcessFunction.getFunction(
          numActiveDimensions, numBasisFunctions, 0.05, new scala.util.Random(2015L))
        val generator = new FourierBasisGaussianProcessGenerator(function, numInactiveDimensions)
        (generator, RandomForestParameters(
          100, true, TreeParameters(
            floor((numActiveDimensions + numInactiveDimensions) / 3).toInt, 10)))
      }
      case other => {
        throw new IllegalArgumentException("Unknown simulation name: " + simulationName)
      }
    }

    val trainingData = dataGenerator.generateData(nTrain, 3.0, scala.util.Random)
    val testData = dataGenerator.generateData(nTest, 0.0, scala.util.Random)

    System.out.println("Training single tree")
    val singleTree = Tree.train(trainingData.indices, trainingData,
      forestParameters.treeParams, 0L)

    System.out.println("Getting predictions")
    val yHatTree = testData.map(x => singleTree.predict(x.features))

    System.out.println("RMSE is: " +
        EvaluationMetrics.rmse(yHatTree, testData.map(_.label)).toString)

    val t0 = System.currentTimeMillis()
    System.out.println("Training random forest")
    val forest = RandomForest.train(trainingData, forestParameters)
    System.out.println("Training time: " + (System.currentTimeMillis() - t0))

    val yHatForest = testData.map(x => forest.predict(x.features))
    System.out.println("RMSE is: " +
        EvaluationMetrics.rmse(yHatForest, testData.map(_.label)).toString)
  }
}
