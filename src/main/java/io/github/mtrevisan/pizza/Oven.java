/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.pizza;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BaseUnivariateSolver;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;


public final class Oven{

	private static final Logger LOGGER = LoggerFactory.getLogger(Oven.class);

	//[°C]
	private static final double DESIRED_BAKED_DOUGH_TEMPERATURE = 73.9;


	/**
	 * [s]
	 *
	 * @see #calculateBakingDuration(Dough, BakingInstruments, double, double, double, double)
	 */
	private static final double SOLVER_BAKING_TIME_MAX = 1800.;
	private static final int SOLVER_EVALUATIONS_MAX = 100;

	//https://www.oreilly.com/library/view/cooking-for-geeks/9781449389543/ch04.html
	private static final double MAILLARD_REACTION_TEMPERATURE = 155.;


	//accuracy is ±1 s
	private final FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(0.1, 1., 1.e-5, 1.e-5);
	private final BaseUnivariateSolver<UnivariateFunction> solverBakingTime = new BracketingNthOrderBrentSolver(1., 5);


	OvenType ovenType;

	double distanceHeaterTop;
	double bakingTemperatureTop;
	double distanceHeaterBottom;
	double bakingTemperatureBottom;


	public static Oven create(final OvenType ovenType) throws OvenException{
		return new Oven(ovenType);
	}

	private Oven(final OvenType ovenType) throws OvenException{
		if(ovenType == null)
			throw OvenException.create("Missing oven type");

		this.ovenType = ovenType;
	}

	public final Oven withDistanceHeaterTop(final double distanceHeater) throws OvenException{
		if(distanceHeater <= 0.)
			throw OvenException.create("Top heater distance from the tray cannot be non-positive");

		distanceHeaterTop = distanceHeater;

		return this;
	}

	public final Oven withDistanceHeaterBottom(final double distanceHeater) throws OvenException{
		if(distanceHeater <= 0.)
			throw OvenException.create("Bottom heater distance from the tray cannot be non-positive");

		distanceHeaterBottom = distanceHeater;

		return this;
	}

	public final void validate() throws OvenException{
		if(ovenType == null)
			throw OvenException.create("Oven type must be given");
		if(distanceHeaterTop == 0. && distanceHeaterBottom == 0.)
			throw OvenException.create("Oven must have at least an heater component");
	}



	/**
	 * @param recipe	Recipe.
	 * @param targetPizzaHeight	Desired pizza height [cm].
	 * @param bakingInstruments	Baking instruments.
	 * @return	The baking instructions.
	 * @throws OvenException	If validation fails.
	 */
	public final BakingInstructions bakeRecipe(final Dough dough, final Recipe recipe, final double targetPizzaHeight,
			final BakingInstruments bakingInstruments) throws OvenException{
		validate();
		bakingInstruments.validate();

		//FIXME
		final double fatDensity = 0.9175;
		final double doughDensity = recipe.density(fatDensity, dough.ingredientsTemperature, dough.atmosphericPressure);
		final double totalDoughVolume = recipe.doughWeight() / doughDensity;
		final double totalBakingPansArea = bakingInstruments.getBakingPansTotalArea();
		//[cm]
		final double initialDoughHeight = totalDoughVolume / totalBakingPansArea;

		//calculate baking temperature:
		final double bakingRatio = targetPizzaHeight / initialDoughHeight;

		//apply inverse Charles-Gay Lussac
		//FIXME the factor accounts for water content and gases produced by levain
		final double bakingTemperature = 1.1781 * bakingRatio * (dough.ingredientsTemperature + Water.ABSOLUTE_ZERO) - Water.ABSOLUTE_ZERO;
		//https://www.campdenbri.co.uk/blogs/bread-dough-rise-causes.php
		if(bakingTemperature < DESIRED_BAKED_DOUGH_TEMPERATURE)
			throw OvenException.create("Cannot bake at such a temperature able to generate a pizza with the desired height");
		//https://bakerpedia.com/processes/maillard-reaction/
		if(bakingTemperature < MAILLARD_REACTION_TEMPERATURE)
			LOGGER.warn("Cannot bake at such a temperature able to generate the Maillard reaction");

		//TODO
		final BakingInstructions instructions = BakingInstructions.create();
		instructions.withBakingTemperature(bakingTemperature);
		if(distanceHeaterTop > 0.)
			bakingTemperatureTop = bakingTemperature;
		if(distanceHeaterBottom > 0.)
			bakingTemperatureBottom = bakingTemperature;
		//FIXME
		//[cm]
		final double cheeseLayerThickness = 0.2;
		//FIXME
		//[cm]
		final double tomatoLayerThickness = 0.05;
		final Duration bakingDuration = calculateBakingDuration(dough, bakingInstruments, initialDoughHeight, cheeseLayerThickness,
			tomatoLayerThickness, DESIRED_BAKED_DOUGH_TEMPERATURE);
		instructions.withBakingDuration(bakingDuration);
		return instructions;
	}

	//TODO account for baking temperature
	// https://www.campdenbri.co.uk/blogs/bread-dough-rise-causes.php
	//initialTemperature is somewhat between params.temperature(UBound(params.temperature)) and params.ambientTemperature
	//volumeExpansion= calculateCharlesGayLussacVolumeExpansion(initialTemperature, params.bakingTemperature)
	private double calculateCharlesGayLussacVolumeExpansion(final double initialTemperature, final double finalTemperature){
		return (finalTemperature + Water.ABSOLUTE_ZERO) / (initialTemperature + Water.ABSOLUTE_ZERO);
	}

	/**
	 * @param dough	Dough data.
	 * @param bakingInstruments	Baking instruments.
	 * @param layerThicknessDough	Initial dough height [cm].
	 * @param layerThicknessMozzarella	Cheese layer thickness [cm].
	 * @param layerThicknessTomato	Tomato layer thickness [cm].
	 * @param desiredBakedDoughTemperature	Brine (contained into the dough) boiling temperature [°C].
	 * @return	Baking duration.
	 */
	private Duration calculateBakingDuration(final Dough dough, final BakingInstruments bakingInstruments, double layerThicknessDough,
			double layerThicknessMozzarella, double layerThicknessTomato, final double desiredBakedDoughTemperature){
		layerThicknessMozzarella /= 100.;
		layerThicknessTomato /= 100.;
		layerThicknessDough /= 100.;
		final ThermalDescriptionODE ode = new ThermalDescriptionODE(layerThicknessMozzarella, layerThicknessTomato, layerThicknessDough,
			OvenType.FORCED_CONVECTION, bakingTemperatureTop, distanceHeaterTop, bakingTemperatureBottom, distanceHeaterBottom,
			dough.ingredientsTemperature, dough.atmosphericPressure, dough.airRelativeHumidity);

		final double bbt = (desiredBakedDoughTemperature - dough.ingredientsTemperature) / (bakingTemperatureTop - dough.ingredientsTemperature);
		final UnivariateFunction f = time -> {
			final double[] y = ode.getInitialState();
			if(time > 0.)
				integrator.integrate(ode, 0., y, time, y);

			//https://blog.thermoworks.com/bread/homemade-bread-temperature-is-key/
			//assure each layer has at least reached the water boiling temperature
			double min = y[0];
			for(int i = 2; i < y.length; i += 2)
				min = Math.min(y[i], min);
			return min - bbt;
		};
		final double time = solverBakingTime.solve(SOLVER_EVALUATIONS_MAX, f, 0., SOLVER_BAKING_TIME_MAX);
		return Duration.ofSeconds((long)time);
	}

}
