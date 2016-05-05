#ifndef LOADER_HPP
#define LOADER_HPP

#include <stdexcept>
#include <vector>
#include "Runtime.hpp"

/**
 * Exception thrown if the loader had a critical failure.
 * The error message can be accessed through the what() function.
 */
class LoaderError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};


/**
 * This is an abstract class that implements the common interface for loading
 * the Runtime Engines used by the Runtime.
 * Each Runtime Engine has their own implementation of this class in a separate
 * folder.
 */
class Loader {

public:
    /// Returns the devices of the engine.
    virtual const DeviceVector &devices();
};

#endif // !LOADER_HPP
