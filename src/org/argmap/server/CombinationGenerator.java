package org.argmap.server;

import java.io.Serializable;

import javax.persistence.Id;

/*
 * Introduction

 The CombinationGenerator Java class systematically generates all combinations 
 of n elements, taken r at a time. The algorithm is described by Kenneth H. Rosen, 
 Discrete Mathematics and Its Applications, 2nd edition (NY: McGraw-Hill, 1991), pp. 284-286.

 The class is very easy to use. Suppose that you wish to generate all possible three-letter
 combinations of the letters "a", "b", "c", "d", "e", "f", "g". Put the letters into an 
 array. Keep calling the combination generator's getNext () method until there are no more
 combinations left. The getNext () method returns an array of integers, which tell you 
 the order in which to arrange your original array of letters. Here is a snippet of code 
 which illustrates how to use the CombinationGenerator class.

 String[] elements = {"a", "b", "c", "d", "e", "f", "g"};
 int[] indices;
 CombinationGenerator x = new CombinationGenerator (elements.length, 3);
 StringBuffer combination;
 while (x.hasMore ()) {
 combination = new StringBuffer ();
 indices = x.getNext ();
 for (int i = 0; i < indices.length; i++) {
 combination.append (elements[indices[i]]);
 }
 System.out.println (combination.toString ());
 }

 Another example of the usage of the CombinationGenerator is shown below in connection with the Zen Archery problem.

 Source Code
 The source code is free for you to use in whatever way you wish.
 */

//--------------------------------------
// Systematically generate combinations.
//--------------------------------------

public class CombinationGenerator implements Serializable {

	/**
	 * added to suppress warnings
	 */
	private static final long serialVersionUID = 1L;

	@Id
	private Long id;

	private  int[] a;
	private  int n;
	private  int r;
	private Integer numLeft;
	private Integer total;

	// ------------
	// Constructor
	// ------------

	public CombinationGenerator(int n, int r) {
		if (r > n) {
			throw new IllegalArgumentException();
		}
		if (n < 1) {
			throw new IllegalArgumentException();
		}
		this.n = n;
		this.r = r;
		a = new int[r];
		Integer nFact = getFactorial(n);
		Integer rFact = getFactorial(r);
		Integer nminusrFact = getFactorial(n - r);
		total = nFact / (rFact * nminusrFact);
		reset();
	}
	
	@SuppressWarnings("unused")
	private CombinationGenerator(){
		
	}

	// ------
	// Reset
	// ------

	public void reset() {
		for (int i = 0; i < a.length; i++) {
			a[i] = i;
		}
		numLeft = new Integer(total.toString());
	}

	// ------------------------------------------------
	// Return number of combinations not yet generated
	// ------------------------------------------------

	public Integer getNumLeft() {
		return numLeft;
	}

	// -----------------------------
	// Are there more combinations?
	// -----------------------------

	public boolean hasMore() {
		return numLeft.compareTo(0) == 1;
	}

	// ------------------------------------
	// Return total number of combinations
	// ------------------------------------

	public Integer getTotal() {
		return total;
	}

	// ------------------
	// Compute factorial
	// ------------------

	private static Integer getFactorial(int n) {
		Integer fact = 1;
		for (int i = n; i > 1; i--) {
			fact = fact * (new Integer(Integer.toString(i)));
		}
		return fact;
	}

	// --------------------------------------------------------
	// Generate next combination (algorithm from Rosen p. 286)
	// --------------------------------------------------------

	public int[] getNext() {

		if (numLeft.equals(total)) {
			numLeft = numLeft - 1;
			return a;
		}

		int i = r - 1;
		while (a[i] == n - r + i) {
			i--;
		}
		a[i] = a[i] + 1;
		for (int j = i + 1; j < r; j++) {
			a[j] = a[i] + j - i;
		}

		numLeft = numLeft - (1);
		return a;

	}
}
