package fr.inria.astor.core.validation.results;

import fr.inria.astor.core.entities.validation.TestCaseVariantValidationResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Matias Martinez
 *
 */
public class TestCasesProgramValidationResult  implements  TestCaseVariantValidationResult {

	int numberFailingTestCases = 0;
	int numberPassingTestCases = 0;

	boolean regressionExecuted = false;
	boolean resultSucess = false;

	/**
	 * Indicates whether where were a problem during the execution that stop
	 * finishing the complete execution , example Infinite loop
	 **/
	boolean executionError = false;

	TestResult testResult;

	public TestCasesProgramValidationResult(TestResult result) {
		super();
		setTestResult(result);
	}

	public TestCasesProgramValidationResult(boolean errorExecution) {
		this.executionError = errorExecution;
		this.testResult = null;
		this.regressionExecuted = false;
		this.resultSucess = false;
		this.numberFailingTestCases = 0;
		this.numberPassingTestCases = 0;
	}

	public TestCasesProgramValidationResult(TestResult result, boolean resultSucess, boolean regressionExecuted) {
		this(result);
		this.regressionExecuted = regressionExecuted;
		this.resultSucess = resultSucess;
	}

	public boolean isSuccessful() {

		return numberFailingTestCases == 0 && this.resultSucess;
	}

	public int getFailureCount() {

		return numberFailingTestCases;
	}

	public boolean isRegressionExecuted() {
		return regressionExecuted;
	}

	public void setRegressionExecuted(boolean regressionExecuted) {
		this.regressionExecuted = regressionExecuted;
	}

	public int getPassingTestCases() {
		return numberPassingTestCases;
	}

	public String toString() {
		return printTestResult(this.getTestResult());
	}

	public TestResult getTestResult() {
		return testResult;
	}

	public void setTestResult(TestResult result) {
		this.testResult = result;
		if (result != null) {
			numberPassingTestCases = result.casesExecuted - result.failures;
			numberFailingTestCases = result.failures;
			resultSucess = (result.casesExecuted == result.failures);
		}
	}

	public Set<String> get_failing_tests(){
		Set<String> failing_tests = new HashSet<>();
		List<String> failures = testResult.getFailures();
		for(String failed:failures){
			String test_id = failed.split("\\(")[0];
			String test_class = failed.split("\\(")[1].split("\\)")[0];
			failing_tests.add(test_class + "." + test_id);

		}

		return failing_tests;
	}


	protected String printTestResult(TestResult result) {
		if (this.executionError || (result == null)) {
			return "|" + false + "|" + 0 + "|" + 0 + "|" + "[]" + "|";
		}
	
		return "|" + result.wasSuccessful() + "|" + result.failures + "|" + result.casesExecuted + "|" + result.failTest
				+ "|";
	}

	@Override
	public int getCasesExecuted() {

		return getPassingTestCases() + getFailureCount();
	}

	public boolean isExecutionError() {
		return executionError;
	}

}
