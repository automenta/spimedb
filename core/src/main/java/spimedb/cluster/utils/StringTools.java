/**
 * Copyright (c) 2013 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package spimedb.cluster.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class StringTools {
	// RegEx to find all punctuation and control characters minus tabs
    private static final Pattern punctctrl = Pattern.compile("\\p{Punct}|[\\x00-\\x08\\x0A-\\x1F\\x7F]");
    
    /***
     * Computes a fingerprint for the string that can be used to cluster similar strings.
     * The fingerprint is computed by:
     * * remove leading and trailing whitespace
     * * change all chars to lowercase
	 * * remove all punctuation and control characters
	 * * split the string into whitespace-separated tokens
	 * * sort the tokens and remove duplicates
	 * * join the tokens back together
	 * * normalize extended western characters to their ASCII representation (for example "gödel" → "godel")
     * @param str the string to fingerprint
     * @return the fingerprint string
     */
    public static String fingerPrint(String str) {
    	// remove surrounding whitespace, turn to lowercase and remove punctuation and ctrl chars
    	String s = stripPunctAndCtrlChars(str.trim().toLowerCase()); 
    	
    	// tokenize string using white space
    	String[] tokens = s.split("\\s+");
    	
    	// sort tokens and combine into new string
    	TreeSet<String> set = new TreeSet<>();
        Collections.addAll(set, tokens);
        
        StringBuilder fingerPrint = new StringBuilder();
        Iterator<String> i = set.iterator();

        while (i.hasNext()) {  
        	fingerPrint.append(i.next());
            if (i.hasNext()) {
            	fingerPrint.append(' ');
            }
        }
        // convert to ASCII representation and return
        return toASCII(fingerPrint.toString());
    }
    
    /***
     * Removes all punctuation and control characters (minus tabs) from the string
     * 
     * @param s
     * @return
     */
    public static String stripPunctAndCtrlChars(String s) {
    	return punctctrl.matcher(s).replaceAll("");
    }
    
    /***
     * Converts a unicode string to ASCII respresentation
     * NOTE: this function deals only with latin-1 supplement and latin-1 extended code charts
     * 
     * @param s
     * @return ASCII version of string
     */
    public static String toASCII(String s) {
        char[] chars = s.toCharArray();
        StringBuilder ascii = new StringBuilder();
        for (char c : chars) {
        	ascii.append(toASCII(c));
        }
        return ascii.toString();
    }
    
    /***
     * Converts a unicode string to ASCII respresentation
     * 
     * NOTE: this function deals only with latin-1 supplement and latin-1 extended code charts
     * 
     * @param s
     * @return ASCII version of string
     */
    public static char toASCII(char c) {
        return switch (c) {
            case '\u00C0', '\u00C1', '\u00C2', '\u00C3', '\u00C4', '\u00C5', '\u00E0', '\u00E1', '\u00E2', '\u00E3', '\u00E4', '\u00E5', '\u0100', '\u0101', '\u0102', '\u0103', '\u0104', '\u0105' ->
                    'a';
            case '\u00C7', '\u00E7', '\u0106', '\u0107', '\u0108', '\u0109', '\u010A', '\u010B', '\u010C', '\u010D' ->
                    'c';
            case '\u00D0', '\u00F0', '\u010E', '\u010F', '\u0110', '\u0111' -> 'd';
            case '\u00C8', '\u00C9', '\u00CA', '\u00CB', '\u00E8', '\u00E9', '\u00EA', '\u00EB', '\u0112', '\u0113', '\u0114', '\u0115', '\u0116', '\u0117', '\u0118', '\u0119', '\u011A', '\u011B' ->
                    'e';
            case '\u011C', '\u011D', '\u011E', '\u011F', '\u0120', '\u0121', '\u0122', '\u0123' -> 'g';
            case '\u0124', '\u0125', '\u0126', '\u0127' -> 'h';
            case '\u00CC', '\u00CD', '\u00CE', '\u00CF', '\u00EC', '\u00ED', '\u00EE', '\u00EF', '\u0128', '\u0129', '\u012A', '\u012B', '\u012C', '\u012D', '\u012E', '\u012F', '\u0130', '\u0131' ->
                    'i';
            case '\u0134', '\u0135' -> 'j';
            case '\u0136', '\u0137', '\u0138' -> 'k';
            case '\u0139', '\u013A', '\u013B', '\u013C', '\u013D', '\u013E', '\u013F', '\u0140', '\u0141', '\u0142' ->
                    'l';
            case '\u00D1', '\u00F1', '\u0143', '\u0144', '\u0145', '\u0146', '\u0147', '\u0148', '\u0149', '\u014A', '\u014B' ->
                    'n';
            case '\u00D2', '\u00D3', '\u00D4', '\u00D5', '\u00D6', '\u00D8', '\u00F2', '\u00F3', '\u00F4', '\u00F5', '\u00F6', '\u00F8', '\u014C', '\u014D', '\u014E', '\u014F', '\u0150', '\u0151' ->
                    'o';
            case '\u0154', '\u0155', '\u0156', '\u0157', '\u0158', '\u0159' -> 'r';
            case '\u015A', '\u015B', '\u015C', '\u015D', '\u015E', '\u015F', '\u0160', '\u0161', '\u017F' -> 's';
            case '\u0162', '\u0163', '\u0164', '\u0165', '\u0166', '\u0167' -> 't';
            case '\u00D9', '\u00DA', '\u00DB', '\u00DC', '\u00F9', '\u00FA', '\u00FB', '\u00FC', '\u0168', '\u0169', '\u016A', '\u016B', '\u016C', '\u016D', '\u016E', '\u016F', '\u0170', '\u0171', '\u0172', '\u0173' ->
                    'u';
            case '\u0174', '\u0175' -> 'w';
            case '\u00DD', '\u00FD', '\u00FF', '\u0176', '\u0177', '\u0178' -> 'y';
            case '\u0179', '\u017A', '\u017B', '\u017C', '\u017D', '\u017E' -> 'z';
            default -> c;
        };
    }
}
