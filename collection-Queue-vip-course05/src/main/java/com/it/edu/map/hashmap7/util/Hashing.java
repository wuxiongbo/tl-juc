package com.it.edu.map.hashmap7.util;

import sun.misc.VM;

import java.util.concurrent.ThreadLocalRandom;

public class Hashing {
    /**
     * Seed value used for each alternative hash calculated.
     */
    //HASHING_SEED是根据时间戳等计算出来的随机数
    private static int HASHING_SEED;

    private static void init() {
        long nanos = System.nanoTime();
        long now = System.currentTimeMillis();
        int SEED_MATERIAL[] = {
                System.identityHashCode(String.class),
                System.identityHashCode(System.class),
                (int) (nanos >>> 32),
                (int) nanos,
                (int) (now >>> 32),
                (int) now,
                (int) (System.nanoTime() >>> 2)
        };

        // Use murmur3 to scramble the seeding material.
        // Inline implementation to avoid loading classes
        int h1 = 0;

        // body
        for (int k1 : SEED_MATERIAL) {
            k1 *= 0xcc9e2d51;
            k1 = (k1 << 15) | (k1 >>> 17);
            k1 *= 0x1b873593;

            h1 ^= k1;
            h1 = (h1 << 13) | (h1 >>> 19);
            h1 = h1 * 5 + 0xe6546b64;
        }

        // tail (always empty, as body is always 32-bit chunks)

        // finalization

        h1 ^= SEED_MATERIAL.length * 4;

        // finalization mix force all bits of a hash block to avalanche
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;

        HASHING_SEED = h1;
    }

    /**
     * Cached value of the alternative hashing algorithm result
     */
    private transient static int hash32 = 0;

    /**
     * Calculates a 32-bit hash value for this string.
     *
     * @return a 32-bit hash value for this string.
     */
    static int hash32() {
        //h==0表示未计算过hash code
        //可以看到hash code只会计算一次
        int h = hash32;
        if (0 == h) {
            // harmless data race on hash32 here.
            h = Hashing.murmur3_32(HASHING_SEED, value, 0, value.length);

            // ensure result is not zero to avoid recalcing
            //确保结果非0，避免重新计算
            h = (0 != h) ? h : 1;

            hash32 = h;
        }

        return h;
    }

    /** The value is used for character storage. */
    private static char value[];

    public static int stringHash32(String key) {
        value = key.toCharArray();
        init();
        return hash32();
    }

    //算法
    public static int randomHashSeed(Object var0) {
        int var1;
        if (VM.isBooted()) {
            var1 = ThreadLocalRandom.current().nextInt();
        } else {
            int[] var2 = new int[]{
                    System.identityHashCode(Hashing.class),
                    System.identityHashCode(var0),
                    System.identityHashCode(Thread.currentThread()),
                    (int)Thread.currentThread().getId(),
                    (int)(System.currentTimeMillis() >>> 2),
                    (int)(System.nanoTime() >>> 5),
                    (int)(Runtime.getRuntime().freeMemory() >>> 4)};
            var1 = murmur3_32(var2);
        }
        return 0 != var1 ? var1 : 1;
    }
    public static int murmur3_32(int[] var0) {
        return murmur3_32(0, (int[])var0, 0, var0.length);
    }
    public static int murmur3_32(int var0, int[] var1, int var2, int var3) {
        int var4 = var0;
        int var5 = var2;

        for(int var6 = var2 + var3; var5 < var6; var4 = var4 * 5 + -430675100) {
            int var7 = var1[var5++];
            var7 *= -862048943;
            var7 = Integer.rotateLeft(var7, 15);
            var7 *= 461845907;
            var4 ^= var7;
            var4 = Integer.rotateLeft(var4, 13);
        }

        var4 ^= var3 * 4;
        var4 ^= var4 >>> 16;
        var4 *= -2048144789;
        var4 ^= var4 >>> 13;
        var4 *= -1028477387;
        var4 ^= var4 >>> 16;
        return var4;
    }
    public static int murmur3_32(int var0, char[] var1, int var2, int var3) {
        int var4 = var0;
        int var5 = var2;

        int var6;
        int var7;
        for(var6 = var3; var6 >= 2; var4 = var4 * 5 + -430675100) {
            var7 = var1[var5++] & '\uffff' | var1[var5++] << 16;
            var6 -= 2;
            var7 *= -862048943;
            var7 = Integer.rotateLeft(var7, 15);
            var7 *= 461845907;
            var4 ^= var7;
            var4 = Integer.rotateLeft(var4, 13);
        }

        if (var6 > 0) {
            char var8 = var1[var5];
            var7 = var8 * -862048943;
            var7 = Integer.rotateLeft(var7, 15);
            var7 *= 461845907;
            var4 ^= var7;
        }

        var4 ^= var3 * 2;
        var4 ^= var4 >>> 16;
        var4 *= -2048144789;
        var4 ^= var4 >>> 13;
        var4 *= -1028477387;
        var4 ^= var4 >>> 16;
        return var4;
    }
}
