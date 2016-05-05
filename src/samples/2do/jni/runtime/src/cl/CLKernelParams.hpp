#ifndef CL_CLKERNELPARAMS_HPP
#define CL_CLKERNELPARAMS_HPP

#include <unordered_map>
#include <string>

/// A hash that stores the position of a given kernel parameter given its name.
typedef std::unordered_map<std::string, unsigned> KernelParamHash;

/// A pair for the KernelParamHash;
typedef std::pair<std::string, unsigned> KernelParamPair;

/**
 * This class encapsulates the interface to get and return the position of
 * a kernel parameter given its name.
 * @todo TODO: support #include directives when building the hash.
 */
class CLKernelParams {
    /// Holds the KernelParamsHash for each different kernel.
    std::unordered_map<std::string, KernelParamHash> _kernelParamsHolder;

public:
    /**
     * Creates a map from a given kernel name to the KernelParamsHash structure
     * containing the parameter's positions.
     */
    CLKernelParams(std::string source);

    /**
     * Get the KernelParamHash of a given kernel.
     */
    inline const KernelParamHash &kernelParams(const std::string &kernel) {
         return _kernelParamsHolder[kernel];
    }
};

#endif // !CL_CLKERNELPARAMS_HPP
