package code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Write an efficient algorithm to find K-complementary pairs in a given array
 * of integers. Given Array A, pair (i, j) is K- complementary if K = A[i] +
 * A[j];
 * 
 * @author andreit
 * 
 */
public class Complementary {

	/**
	 * Used to store the result pairs
	 */
	private static class Pair {
		public int i;
		public int j;

		public Pair(int i, int j) {
			super();
			this.i = i;
			this.j = j;
		}

		@Override
		public String toString() {
			return "(i=" + i + ", j=" + j + ")";
		}
	}

	public static Collection<Pair> getComplementaryPairs(int listOfValues[], int sum) {

		if (listOfValues == null) {
			return new ArrayList<>();
		}

		// Sacrifice some memory for speed
		Map<Integer, Integer> complmap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> indexmap = new HashMap<Integer, Integer>();

		// O(n)
		for (int index = 0; index < listOfValues.length; index++) {
			int number = listOfValues[index];
			long complement = sum - number; // Overflow possibility
			complmap.put(number, (int) complement);
			indexmap.put(number, index);
		}

		// O(n)
		Collection<Pair> tuples = new ArrayList<>();
		for (int number : listOfValues) {
			long complement = sum - number;// Overflow possibility
			if (complmap.containsKey((int) complement)) {
				int i = indexmap.get(number);
				int j = indexmap.get((int) complement);
				tuples.add(new Pair(i, j));
			}
		}

		// Total complexity O(n) + O(n) ==> O(n)

		return tuples;

	}

	public static void main(String[] args) {
		System.out.println(getComplementaryPairs(new int[] { 7, 1, 5, 6, 9, 3, 11, -1 }, 10));
		System.out.println(getComplementaryPairs(new int[] { 7, 1, 5, 6, 9, 3, 11, -1 }, -1));
	}

}
