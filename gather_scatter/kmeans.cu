// includes, system
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

// includes CUDA
#include <cuda_runtime.h>

// includes, project
#include <helper_cuda.h>
#include <helper_functions.h> // helper functions for SDK examples

// includes thrust
#include <thrust/host_vector.h>
#include <thrust/device_vector.h>
#include <thrust/fill.h>
#include <thrust/reduce.h>
#include <thrust/iterator/transform_iterator.h>
#include <thrust/iterator/counting_iterator.h>
#include <thrust/iterator/discard_iterator.h>
#include <thrust/copy.h>
#include <thrust/execution_policy.h>

using namespace thrust::placeholders;

////////////////////////////////////////////////////////////////////////////////
// Inline functions
////////////////////////////////////////////////////////////////////////////////

inline __device__ float calculateDistanceSquared(
    float x1, float y1, float z1,
    float x2, float y2, float z2) {
    return (x1 - x2)*(x1 - x2) +
           (y1 - y2)*(y1 - y2) +
           (z1 - z2)*(z1 - z2);
}

inline float random_float(){ 
    return (float)rand()/(float)RAND_MAX; 
}

////////////////////////////////////////////////////////////////////////////////
// Kernels
////////////////////////////////////////////////////////////////////////////////

__global__ void
calculateDistances(float *points_x, float* points_y, float* points_z,
        float *centroid_x, float* centroid_y, float* centroid_z,
        int *closest, int k, int n)
{
    const unsigned int tid = threadIdx.x + blockIdx.x * blockDim.x;
    if(tid < n){
        unsigned index_min;
        float min_distance = 10e9;
        for(int i = 0; i < k; i++){
            float distance = calculateDistanceSquared(
                    points_x[tid], points_y[tid], points_z[tid],
                    centroid_x[i], centroid_y[i], centroid_z[i]);
            if(min_distance > distance){
                index_min = i;
                min_distance = distance;
            }
        }
        closest[tid] = index_min;
    }
}

__global__ void reduce(
        float *points_x, float* points_y, float* points_z,
        float *centroid_x, float* centroid_y, float* centroid_z,
        int *closest, int *sums, int n) {
    const unsigned int tid = threadIdx.x + blockIdx.x * blockDim.x;
    if(tid < n){
        int centroid_num = closest[tid];
        atomicAdd(&centroid_x[centroid_num], points_x[tid]);
        atomicAdd(&centroid_y[centroid_num], points_y[tid]);
        atomicAdd(&centroid_z[centroid_num], points_z[tid]);
        atomicAdd(&sums[centroid_num], 1);
    }
}

__global__ void
calculateMean(float *centroid_x, float* centroid_y, float* centroid_z, int *sums, int k)
{
    const unsigned int tid = threadIdx.x + blockIdx.x * blockDim.x;
    if(tid < k){
        int sum = sums[tid];
        if(sum != 0){
            centroid_x[tid] /= sums[tid];
            centroid_y[tid] /= sums[tid];
            centroid_z[tid] /= sums[tid];
        } else {
            centroid_x[tid] = 0.0f;
            centroid_y[tid] = 0.0f;
            centroid_z[tid] = 0.0f;
        }
    }
}

////////////////////////////////////////////////////////////////////////////////
// Function headers
////////////////////////////////////////////////////////////////////////////////

void randomize(thrust::host_vector<float>& point_x, thrust::host_vector<float>& point_y, thrust::host_vector<float>& point_z,
        thrust::host_vector<float>& centroid_x, thrust::host_vector<float>& centroid_y, thrust::host_vector<float>& centroid_z,
        int k, int n);

bool stop(thrust::host_vector<float>& h_centroid_x, thrust::host_vector<float>& h_centroid_y, thrust::host_vector<float>& h_centroid_z,
        thrust::device_vector<float>& d_centroid_x, thrust::device_vector<float>& d_centroid_y, thrust::device_vector<float>& d_centroid_z,
        int k, float epsilon);

void write(thrust::host_vector<float>& h_x, thrust::host_vector<float>& h_y, thrust::host_vector<float>& h_z, int n, const char* filename);

////////////////////////////////////////////////////////////////////////////////
// Program main
////////////////////////////////////////////////////////////////////////////////

int main(int argc, char **argv){
    srand(0);
    //setup parameters
    int k = 8, n = 3200000;
    float epsilon = 0.0001f;
    //initialize host vectors
    thrust::host_vector<float> h_points_x(n), h_points_y(n), h_points_z(n);
    thrust::host_vector<float> h_centroids_x(k), h_centroids_y(k), h_centroids_z(k);
    //generate data
    randomize(h_points_x, h_points_y, h_points_z, h_centroids_x, h_centroids_y, h_centroids_z, k, n);
    //initialize device vectors, copy data from host vectors
    thrust::device_vector<float> d_points_x(h_points_x), d_points_y(h_points_y), d_points_z(h_points_z);
    thrust::device_vector<float> d_centroids_x = h_centroids_x, d_centroids_y = h_centroids_y, d_centroids_z = h_centroids_z;
    thrust::device_vector<int> d_closest(n);
    thrust::device_vector<int> d_sums(k);
    //start timers
    StopWatchInterface *timer = 0;
    sdkCreateTimer(&timer);
    sdkStartTimer(&timer);

    // setup execution parameters
    dim3  grid(n / 256 + 1, 1, 1);
    dim3  threads(256, 1, 1);

    dim3  grid2(k / 1024 + 1, 1, 1);
    dim3  threads2(1024, 1, 1);

    int iter = 0;
    do {
        thrust::fill(d_closest.begin(), d_closest.end(), 0);

        //for each point in data set find closest centroid
        calculateDistances<<< grid, threads >>>(
                thrust::raw_pointer_cast(&d_points_x[0]),
                thrust::raw_pointer_cast(&d_points_y[0]),
                thrust::raw_pointer_cast(&d_points_z[0]),
                thrust::raw_pointer_cast(&d_centroids_x[0]),
                thrust::raw_pointer_cast(&d_centroids_y[0]),
                thrust::raw_pointer_cast(&d_centroids_z[0]),
                thrust::raw_pointer_cast(&d_closest[0]),
                k, n);

        cudaDeviceSynchronize();
        getLastCudaError("Kernel execution failed");
        
        //clear old centroids data
        thrust::fill(d_centroids_x.begin(), d_centroids_x.end(), 0.0f);
        thrust::fill(d_centroids_y.begin(), d_centroids_y.end(), 0.0f);
        thrust::fill(d_centroids_z.begin(), d_centroids_z.end(), 0.0f);
        thrust::fill(d_sums.begin(), d_sums.end(), 0);

        //sum up for each centroid distance to point from point's perspective
        reduce<<< grid, threads >>>(
                thrust::raw_pointer_cast(&d_points_x[0]),
                thrust::raw_pointer_cast(&d_points_y[0]),
                thrust::raw_pointer_cast(&d_points_z[0]),
                thrust::raw_pointer_cast(&d_centroids_x[0]),
                thrust::raw_pointer_cast(&d_centroids_y[0]),
                thrust::raw_pointer_cast(&d_centroids_z[0]),
                thrust::raw_pointer_cast(&d_closest[0]),
                thrust::raw_pointer_cast(&d_sums[0]),
                n);
        
        cudaDeviceSynchronize();
        getLastCudaError("Kernel execution failed");

        //now calculate mean from the previously calculated sum it is a new centroid
        calculateMean<<< grid2, threads2 >>>(
                thrust::raw_pointer_cast(&d_centroids_x[0]),
                thrust::raw_pointer_cast(&d_centroids_y[0]),
                thrust::raw_pointer_cast(&d_centroids_z[0]),
                thrust::raw_pointer_cast(&d_sums[0]), k);

        cudaDeviceSynchronize();
        getLastCudaError("Kernel execution failed");
        //one iteration done
        iter = iter + 1;
    } while(
        //check if change is small compared to the last iteration
        !stop(h_centroids_x, h_centroids_y, h_centroids_z, 
            d_centroids_x, d_centroids_y, d_centroids_z,
            k, epsilon) || iter > 100);

    //stop timers and print summary
    sdkStopTimer(&timer);
    printf("Processing time: %f (ms), %d iterations\n", sdkGetTimerValue(&timer), iter);
    sdkDeleteTimer(&timer);
    
    //write output of the program to a file
    write(h_points_x, h_points_y, h_points_z, n, "points.txt");
    write(h_centroids_x, h_centroids_y, h_centroids_z, k, "centroids.txt");

    printf("Exiting...\n");
    exit(EXIT_SUCCESS);
}

//generate data
void randomize(thrust::host_vector<float>& point_x, thrust::host_vector<float>& point_y, thrust::host_vector<float>& point_z,
    thrust::host_vector<float>& centroid_x, thrust::host_vector<float>& centroid_y, thrust::host_vector<float>& centroid_z,
    int k, int n){
    for(int i = 0; i < k; i++){
        float x = random_float();
        float y = random_float();
        float z = random_float();
        centroid_x[i] = x;
        centroid_y[i] = y;
        centroid_z[i] = z;
    }
    for(int i = 0; i < n; i++){
        float x = random_float();
        float y = random_float();
        float z = random_float();
        point_x[i] = x;
        point_y[i] = y;
        point_z[i] = z;
    }
}

//check if alghoritm should stop, i.e. if norm of centroids vector is lesser
//than given epsilon
bool stop(thrust::host_vector<float>& h_centroid_x, thrust::host_vector<float>& h_centroid_y, thrust::host_vector<float>& h_centroid_z,
    thrust::device_vector<float>& d_centroid_x, thrust::device_vector<float>& d_centroid_y, thrust::device_vector<float>& d_centroid_z,
    int k, float epsilon){
    thrust::host_vector<float> 
        h_centroid_x_new(d_centroid_x), 
        h_centroid_y_new(d_centroid_y), 
        h_centroid_z_new(d_centroid_z);
    float norm = 0.0f;
    for(int i = 0; i < k; i++){
        norm += abs(h_centroid_x_new[i] - h_centroid_x[i]) + 
                abs(h_centroid_y_new[i] - h_centroid_y[i]) + 
                abs(h_centroid_z_new[i] - h_centroid_z[i]);
    }
    norm /= (k * 3);
    h_centroid_x = h_centroid_x_new;
    h_centroid_y = h_centroid_y_new;
    h_centroid_z = h_centroid_z_new;
    printf("norm: %f\n", norm);
    if(norm > epsilon) return false;
    else return true;
}

// writes vectors to a specified file
void write(thrust::host_vector<float>& h_x, thrust::host_vector<float>& h_y, thrust::host_vector<float>& h_z, int n, const char* filename){
    std::ofstream myfile;
    myfile.open(filename);
    for(int i = 0; i < n; i++){
        myfile << h_x[i] << " " << h_y[i] << " " << h_z[i] << " " << std::endl;
    }
    myfile.close();
}
