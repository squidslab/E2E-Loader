package Services.Dependencies;
/**
 * Utility class to calculate the Levenshtein distance between two strings.
 *
 * <p>The Levenshtein distance is a measure of the minimum number of single-character
 * edits (insertions, deletions, or substitutions) required to change one string
 * into another.
 */
public class LevenshteinDistance {
    /**
     * Computes the Levenshtein distance between two strings.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the number of edits required to convert s1 into s2
     */
    public static int levenshteinDistance(String s1, String s2) {
        return dist( s1.toCharArray(), s2.toCharArray());
    }


    /**
     * Computes the Levenshtein distance between two character arrays.
     *
     * <p>This implementation uses a memory-efficient approach by storing only the
     * previous row of the distance matrix.
     *
     * @param s1 the first character array
     * @param s2 the second character array
     * @return the number of edits required to convert s1 into s2
     */
    public static int dist( char[] s1, char[] s2 ) {

        // memoize only previous line of distance matrix
        int[] prev = new int[ s2.length + 1 ];

        for( int j = 0; j < s2.length + 1; j++ ) {
            prev[ j ] = j;
        }

        for( int i = 1; i < s1.length + 1; i++ ) {

            // calculate current line of distance matrix
            int[] curr = new int[ s2.length + 1 ];
            curr[0] = i;

            for( int j = 1; j < s2.length + 1; j++ ) {
                int d1 = prev[ j ] + 1;
                int d2 = curr[ j - 1 ] + 1;
                int d3 = prev[ j - 1 ];
                if ( s1[ i - 1 ] != s2[ j - 1 ] ) {
                    d3 += 1;
                }
                curr[ j ] = Math.min( Math.min( d1, d2 ), d3 );
            }

            // define current line of distance matrix as previous
            prev = curr;
        }
        return prev[ s2.length ];
    }
}
