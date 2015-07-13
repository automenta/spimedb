/*
 * pack64.pack: encodes a vector into a pack64'd string.
 * pack64.unpack: decodes a pack64'd string into a vector.
 *
 * This library is for decoding a packed vector format, defined in the Python
 * package `pack64`.
 *
 * The format uses URL-safe base64 to encode an exponent followed by several
 * 18-bit signed integers.
 */
(function (name, global, definition) {
    if (typeof module !== 'undefined') {
        module.exports = definition();
    } else if (typeof define !== 'undefined' && typeof define.amd === 'object') {
        define(definition);
    } else {
        global[name] = definition();
    }
})('pack64', this, function () {
    "use strict";

    // 2^17 is the number that makes an 18-bit signed integer go negative.
    var SIGN_BIT = 131072;
    var ROUND_MARGIN = SIGN_BIT / (SIGN_BIT - 0.5);

    var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    var alphabet_map = {};
    for (var i = 0; i < 64; i++) {
        alphabet_map[alphabet.charAt(i)] = i;
    }

    function pack(vec) {
        // Calculate the smallest power of 2 we *don't* need to represent.
        // The exponent we want will be 17 lower than that.
        var max = 0, i;
        for (i = 0; i < vec.length; i++) {
            var v = Math.abs(vec[i]) * ROUND_MARGIN;
            if (v > max) {
                max = v;
            }
        }

        var upperBound = Math.floor(1 + Math.log(max) / Math.log(2));
        var exponent = upperBound - 17;
        if (exponent > 23) {
            // Overflow. Return the flag vector for "almost infinity".
            return '-';
        }

        if (exponent < -40) {
            // Underflow. Lose some precision. Or maybe all of it.
            exponent = -40;
        }

        var power = Math.pow(2, exponent);
        var res = [alphabet[exponent + 40]];
        for (i = 0; i < vec.length; i++) {
            var num = Math.round(vec[i] / power);
            if (num < 0) {
                num += SIGN_BIT*2;
            }
            // Do the signed arithmetic to represent an 18-bit integer.
            res.push(alphabet[(num % (1 << 18)) >> 12]);
            res.push(alphabet[(num % (1 << 12)) >> 6]);
            res.push(alphabet[num % (1 << 6)]);
        }

        return res.join('');
    }

    function unpack(str) {
        var hexes = [];
        for (var i = 0; i < str.length; i++) {
            hexes[i] = alphabet_map[str.charAt(i)];
        }

        var K = (hexes.length - 1) / 3;
        var vector = new Float32Array(Math.ceil(K));
        var unit = Math.pow(2, hexes[0] - 40);
        for (var i = 0; i < K; i++) {
            var base = i * 3
            var integer = 4096*hexes[base + 1] + 64*hexes[base + 2] + hexes[base + 3];
            if (integer >= SIGN_BIT) {
                integer -= SIGN_BIT * 2;
            }
            vector[i] = integer * unit;
        }

        return vector;
    }

    function test() {

        function assertPackedEquals(v, str) {
            console.assert(pack(v) == str, "pack([" + v + "]) == " + str)
        }

        assertPackedEquals([ ], 'A')
        assertPackedEquals([0], 'AAAA')
        assertPackedEquals([1],'YQAA')
        assertPackedEquals([-1, 1], 'YwAAQAA')
        assertPackedEquals([Math.pow(2, 16), -1], 'oQAA___')
        assertPackedEquals([Math.pow(2, 16),Math.pow(2, 17) - 1], 'oQAAf__')
        assertPackedEquals([Math.pow(2, 16),Math.pow(2, 17) - 0.2], 'pIAAQAA')
        assertPackedEquals([Math.pow(2, 16), Math.pow(-2, 17) + 0.2], 'pIAAwAA')
        assertPackedEquals([Math.pow(2, 20), -1], 'sQAAAAA')

        function assertRoundtripIsEqual(v) {
            var roundtripped = unpack(pack(v))

            for (var i = 0; i < roundtripped.length; i++) {
                console.assert(roundtripped[i] == v[i],
                    "pack(unpack([" + v + "])) == [" + v + "]")
            }

            console.assert(roundtripped.length == v.length,
                "The length of the vector stays the same after a unpack(pack(v)) cycle. ([" + v + "] != [" + roundtripped + "]) ")
        }
        assertRoundtripIsEqual([1, 2, 3])
        assertRoundtripIsEqual([])
        assertRoundtripIsEqual([0, 0, 0])
        assertRoundtripIsEqual([-1])
        assertRoundtripIsEqual([1])
        assertRoundtripIsEqual([1, 2])
        assertRoundtripIsEqual([.5, .25])

    }

    return { pack: pack, unpack: unpack, test: test };
});