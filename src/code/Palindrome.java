package code;

/**
 * Write an efficient algorithm to check if a string is a palindrome. A string
 * is a palindrome if the string matches the reverse of string. Example: 1221 is
 * a palindrome but not 1121.
 * 
 * @author andreit
 *
 */
public class Palindrome {

	public static boolean check(String input) {

		// Total complexity O(n/2) ==> O(n)
		int length = input.length();
		for (int i = 0; i < length / 2; i++) {
			if (input.charAt(i) != input.charAt(length - i - 1)) {
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {

		System.out.println(check("aabaa"));
		System.out.println(check("aabbaa"));
		System.out.println(check("aabaaf"));
		System.out.println(check("1121"));
		System.out.println(check("1221"));
	}

}
