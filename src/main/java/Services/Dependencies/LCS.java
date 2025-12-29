package Services.Dependencies;
/**
 * Utility class for computing the Longest Common Substring (LCS) similarity
 * between two character sequences.
 *
 * <p>This implementation returns a normalized similarity value between 0 and 1,
 * calculated as the length of the longest common substring divided by the
 * maximum length of the two input sequences.
 */
public class LCS {

    /**
     * Computes the normalized longest common substring (LCS) similarity between two character arrays.
     *
     * <p>The method uses dynamic programming to calculate the length of the longest
     * contiguous substring shared by {@code X} and {@code Y}. The result is then
     * normalized by dividing by the maximum length of the two input arrays.
     *
     * @param X first character array
     * @param Y second character array
     * @param m length of the first array {@code X}
     * @param n length of the second array {@code Y}
     * @return a float value between 0 and 1 representing the normalized longest common substring similarity
     */
    public static float LCSubStr(char X[], char Y[], int m, int n)
    {
        // Create a table to store
        // lengths of longest common
        // suffixes of substrings.
        // Note that LCSuff[i][j]
        // contains length of longest
        // common suffix of
        // X[0..i-1] and Y[0..j-1].
        // The first row and first
        // column entries have no
        // logical meaning, they are
        // used only for simplicity of program
        int LCStuff[][] = new int[m + 1][n + 1];

        // To store length of the longest
        // common substring
        int result = 0;

        // Following steps build
        // LCSuff[m+1][n+1] in bottom up fashion
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0)
                    LCStuff[i][j] = 0;
                else if (X[i - 1] == Y[j - 1]) {
                    LCStuff[i][j]
                            = LCStuff[i - 1][j - 1] + 1;
                    result = Integer.max(result,
                            LCStuff[i][j]);
                }
                else
                    LCStuff[i][j] = 0;
            }
        }
        return (float) result/Math.max(m,n);
    }

}
