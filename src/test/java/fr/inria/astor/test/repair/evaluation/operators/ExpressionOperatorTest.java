package fr.inria.astor.test.repair.evaluation.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import fr.inria.astor.approaches.jgenprog.JGenProg;
import fr.inria.astor.approaches.jgenprog.operators.ExpressionReplaceOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.loop.spaces.ingredients.AstorIngredientSpace;
import fr.inria.astor.core.loop.spaces.ingredients.scopes.AstorCtIngredientSpace;
import fr.inria.astor.core.loop.spaces.ingredients.scopes.ExpressionIngredientSpace;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.filters.ExpressionIngredientSpaceProcessor;
import fr.inria.astor.test.repair.evaluation.regression.MathTests;
import fr.inria.astor.util.CommandSummary;
import fr.inria.main.evolution.AstorMain;
import fr.inria.main.evolution.ExtensionPoints;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtCodeElement;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtClass;

/**
 * 
 * @author Matias Martinez
 *
 */
public class ExpressionOperatorTest {

	protected Logger log = Logger.getLogger(ExpressionOperatorTest.class.getName());

	/**
	 * This test checks if astor is able to manipulate expression as element in
	 * a modification point
	 * 
	 * @throws Exception
	 */
	@Test
	public void testM70ExpressionAsModificationPoint() throws Exception {

		CommandSummary command = MathTests.getMath70Command();
		command.command.put("-parameters", ExtensionPoints.INGREDIENT_PROCESSOR.identifier + File.pathSeparator
				+ ExpressionIngredientSpaceProcessor.class.getCanonicalName());
		command.command.put("-maxgen", "0");// Avoid evolution

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		List<ProgramVariant> variantss = main1.getEngine().getVariants();
		assertTrue(variantss.size() > 0);

		JGenProg jgp = (JGenProg) main1.getEngine();
		AstorIngredientSpace ingSpace = (AstorIngredientSpace) jgp.getIngredientStrategy().getIngredientSpace();

		int i = 0;
		for (ModificationPoint modpoint : variantss.get(0).getModificationPoints()) {
			// System.out.println("--> "+(i++)+" " +modpoint.getCodeElement());

		}
		ModificationPoint modificationPoint = variantss.get(0).getModificationPoints().get(14);
		CtExpression originalExp = (CtExpression) modificationPoint.getCodeElement();
		assertEquals("i < (maximalIterationCount)", originalExp.toString());
		CtClass parentClass = originalExp.getParent(CtClass.class);
		String parentClassString = parentClass.toString();

		assertNotNull(parentClass);
		// Let's mutate the expression
		CtExpression clonedExp = (CtExpression) MutationSupporter.clone(originalExp);
		CtBinaryOperator binOpExpr = (CtBinaryOperator) clonedExp;
		// Let's check that the operator that will be inserted is not equal to
		// the current.
		assertNotEquals(BinaryOperatorKind.GT, binOpExpr.getKind());
		// Update operator
		binOpExpr.setKind(BinaryOperatorKind.GT);

		ExpressionReplaceOperator expOperator = new ExpressionReplaceOperator();
		OperatorInstance expOperatorInstance = new OperatorInstance(modificationPoint, expOperator, originalExp,
				clonedExp);

		boolean applied = expOperatorInstance.applyModification();
		assertTrue(applied);

		assertNotEquals(clonedExp, originalExp);
		assertEquals("i > (maximalIterationCount)", clonedExp.toString());
		assertEquals("i > (maximalIterationCount)", modificationPoint.getCodeElement().toString());

		// Let's check that the mutated class is different to the original
		String mutatedClassString = parentClass.toString();
		assertNotEquals(parentClassString, mutatedClassString);

		// Undo operator, we should have the original class
		boolean undo = expOperatorInstance.undoModification();
		String revertedChangeClassString = parentClass.toString();
		assertTrue(undo);
		assertEquals("i < (maximalIterationCount)", modificationPoint.getCodeElement().toString());

		assertEquals(parentClassString, revertedChangeClassString);

	}

	/**
	 * This test checks if it works fine a new operator that works at the
	 * expression level.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testM70ExpressionOperator() throws Exception {

		CommandSummary command = MathTests.getMath70Command();
		command.command.put("-parameters", ExtensionPoints.INGREDIENT_PROCESSOR.identifier + File.pathSeparator
				+ ExpressionIngredientSpaceProcessor.class.getCanonicalName());
		command.command.put("-maxgen", "0");// Avoid evolution
		command.command.put("-customop", ExpressionReplaceOperator.class.getName());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		List<ProgramVariant> variantss = main1.getEngine().getVariants();
		assertTrue(variantss.size() > 0);

		JGenProg engine = (JGenProg) main1.getEngine();
		ModificationPoint modificationPoint = variantss.get(0).getModificationPoints().get(14);

		assertEquals("i < (maximalIterationCount)", modificationPoint.getCodeElement().toString());

		// Let's test the creation of a operator instance.
		OperatorInstance opInstance = engine.createOperatorInstanceForPoint(modificationPoint);

		assertNotNull(opInstance);
		assertEquals(ExpressionReplaceOperator.class.getName(), opInstance.getOperationApplied().getClass().getName());

		assertNotNull("Operator replacement needs an ingredient", opInstance.getModified());

		log.debug("Op instance created " + opInstance);
		log.debug("Op instance Ingredient:  " + opInstance.getModified());

		// Let's inspect the ingredient space:
		AstorCtIngredientSpace ingredientSpace = (AstorCtIngredientSpace) engine.getIngredientStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);

		assertTrue(CtBinaryOperator.class.isInstance(opInstance.getOriginal()));
		log.debug("\n Type ingredient: " + ((CtBinaryOperator) opInstance.getOriginal()).getType());
		assertTrue(CtBinaryOperator.class.isInstance(opInstance.getModified()));

		CtBinaryOperator binOpIngredient = (CtBinaryOperator) opInstance.getModified();
		log.debug("\n Type ingredient: " + binOpIngredient.getType());

	}

	/**
	 * This test uses a new ingredient space specially created to manage expressions.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testM70ExpressionOperatorSpaceExpression() throws Exception {

		CommandSummary command = MathTests.getMath70Command();
		command.command.put("-parameters", ExtensionPoints.INGREDIENT_PROCESSOR.identifier + File.pathSeparator
				+ ExpressionIngredientSpaceProcessor.class.getCanonicalName());
		command.command.put("-maxgen", "0");// Avoid evolution
		command.command.put("-customop", ExpressionReplaceOperator.class.getName());
		command.command.put("-scope", ExpressionIngredientSpace.class.getName());

		AstorMain main1 = new AstorMain();
		main1.execute(command.flat());

		List<ProgramVariant> variantss = main1.getEngine().getVariants();
		assertTrue(variantss.size() > 0);

		JGenProg engine = (JGenProg) main1.getEngine();
		ModificationPoint modificationPoint = variantss.get(0).getModificationPoints().get(14);

		assertEquals("i < (maximalIterationCount)", modificationPoint.getCodeElement().toString());

		// Let's inspect the ingredient space:
		ExpressionIngredientSpace ingredientSpace = (ExpressionIngredientSpace) engine.getIngredientStrategy()
				.getIngredientSpace();
		assertNotNull(ingredientSpace);
		assertTrue(ExpressionIngredientSpace.class.isInstance(ingredientSpace));

		log.debug("keys: \n" + ingredientSpace.getLocations());

		log.debug("Ingredient \n:" + ingredientSpace.getAllIngredients());

		// Let's test the creation of a operator instance.
		OperatorInstance opInstance = engine.createOperatorInstanceForPoint(modificationPoint);

		assertNotNull(opInstance);
		assertEquals(ExpressionReplaceOperator.class.getName(), opInstance.getOperationApplied().getClass().getName());

		assertNotNull("Operator replacement needs an ingredient", opInstance.getModified());

		log.debug("Op instance created " + opInstance);
		log.debug("Op instance Ingredient:  " + opInstance.getModified());

		assertTrue(CtBinaryOperator.class.isInstance(opInstance.getOriginal()));
		log.debug("\n Type ingredient: " + ((CtBinaryOperator) opInstance.getOriginal()).getType());
		assertTrue(CtBinaryOperator.class.isInstance(opInstance.getModified()));

		CtBinaryOperator binOpIngredient = (CtBinaryOperator) opInstance.getModified();
		log.debug("\n Type ingredient: " + binOpIngredient.getType());

		assertEquals(((CtBinaryOperator) opInstance.getOriginal()).getType(), binOpIngredient.getType());

		List<CtCodeElement> ingredients = ingredientSpace.getIngredients(opInstance.getOriginal(),
				ExpressionReplaceOperator.class.getName());

		// let's check all ingredients
		for (CtCodeElement ingredient : ingredients) {
			assertTrue(CtBinaryOperator.class.isInstance(ingredient));
			assertEquals(((CtBinaryOperator) opInstance.getOriginal()).getType(),
					((CtBinaryOperator) ingredient).getType());
		}

	}

}