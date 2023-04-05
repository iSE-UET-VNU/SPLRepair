package fr.inria.astor.core.faultlocalization.entity;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.faultlocalization.gzoltar.TestCaseResult;
import org.apache.maven.surefire.shared.lang3.ObjectUtils;

/**
 * This entity represents a suspicious lines inside a class.
 * 
 * @author Matias Martinez, matias.martinez@inria.fr
 * 
 */
public class SuspiciousCode {
	/**
	 * Suspicious class
	 */
	String className;

	String methodName;
	/**
	 * Suspicious line number
	 */
	int lineNumber;
	String featureInfo = null;
	/**
	 * Suspicious value of the line
	 */
	double suspiciousValue;

	String fileName;

	/**
	 * Key is the test identifier, value Numbers of time executed by that test.
	 */
	private Map<Integer, Integer> coverage = null;

	protected List<TestCaseResult> coveredByTests = null;

	public SuspiciousCode() {
	}

	public SuspiciousCode(String className, String methodName, int lineNumber, double susp,
			Map<Integer, Integer> frequency) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.suspiciousValue = susp;
		this.coverage = frequency;
	}

	public SuspiciousCode(String className, String methodName, int lineNumber, String featureInfo, double susp,
						  Map<Integer, Integer> frequency) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.featureInfo = featureInfo;
		this.suspiciousValue = susp;
		this.coverage = frequency;
	}

	public SuspiciousCode(String className, String methodName, double susp) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.suspiciousValue = susp;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public double getSuspiciousValue() {
		return suspiciousValue;
	}

	DecimalFormat df = new DecimalFormat("#.###");

	public String getSuspiciousValueString() {
		return df.format(this.suspiciousValue);
	}

	public void setSusp(double susp) {
		this.suspiciousValue = susp;
	}

	public String getClassName() {
		int i = className.indexOf("$");
		if (i != -1) {
			return className.substring(0, i);
		}
		String[] class_name_tmp = className.split("\\.");
		return class_name_tmp[class_name_tmp.length -2] + "." + class_name_tmp[class_name_tmp.length -1];
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}
	public String getFeatureInfo(){
		return featureInfo;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public String toString() {
		return "Candidate [className=" + className + ", methodName=" + methodName + ", lineNumber=" + lineNumber
				+ ", featureLevelInfo=" + featureInfo + ", susp=" + suspiciousValue + "]";
	}

	public Map<Integer, Integer> getCoverage() {
		return coverage;
	}

	public void setCoverage(Map<Integer, Integer> coverage) {
		this.coverage = coverage;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public List<TestCaseResult> getCoveredByTests() {
		return coveredByTests;
	}

	public void setCoveredByTests(List<TestCaseResult> coveredByTests) {
		this.coveredByTests = coveredByTests;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SuspiciousCode other = (SuspiciousCode) obj;
		if(featureInfo != null){
			if(other.featureInfo == null){
				return false;
			}else if(other.featureInfo.equals(featureInfo)){
				return true;
			}
		}
		else{
			if(className != null){
				if(other.className == null) return false;
				else if(other.className.equals(className)){
					return lineNumber == other.lineNumber;
				}
			}else {
				return other.className == null;
			}
		}
		return false;
	}

}
