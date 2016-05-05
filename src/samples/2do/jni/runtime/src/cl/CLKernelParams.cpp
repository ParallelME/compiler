#include "CLKernelParams.hpp"
#include "../util/string.hpp"
#include <algorithm>
#include <regex>

CLKernelParams::CLKernelParams(std::string source) {
    removeNewlines(source);

    // Get kernel name on \1 and content between parenthesis on \2.
    std::regex e1(R"(kernel\s+void\s+(\w+)\s*\((.*?)\))");

    // Get kernel parameter names on content between parenthesis with a comma
    // appended to it.
    std::regex e2(R"([\s\*](\w+)\,)");

    std::sregex_iterator kernelIt(source.cbegin(), source.cend(), e1);
    std::sregex_iterator kernelEnd;
    for(; kernelIt != kernelEnd; ++kernelIt) {
        // kernelIt->str(1) is the kernel name, kernelIt->str(2) the content
        // between parenthesis.
        std::string params(kernelIt->str(2));
        trim(params).append(",");

        // Our new KernelParamHash.
        KernelParamHash paramHash;

        std::sregex_token_iterator paramIt(params.cbegin(), params.cend(), e2, {1});
        std::sregex_token_iterator paramEnd;
        for(unsigned i = 0; paramIt != paramEnd; ++paramIt, ++i)
            paramHash.insert(KernelParamPair(paramIt->str(), i));

        // Save the KernelParamHash by the kernel's name, if it is not already
        // there.
        if(_kernelParamsHolder.find(kernelIt->str(1)) == _kernelParamsHolder.end())
            _kernelParamsHolder.insert(std::pair<std::string, KernelParamHash>(
                     kernelIt->str(1), paramHash));
    }
}
