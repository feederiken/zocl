// Copyright 2010 Jeff Heaton
// LGPL-3.0

__kernel void sampleKernel(__global const float *a, __global const float *b, __global float *c) {
    int gid = get_global_id(0);
    c[gid] = a[gid] + b[gid];
}
