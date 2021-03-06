\name{str.H2OParsedDataVA}
\alias{str.H2OParsedDataVA}
\title{
Display the Structure of a H2O ValueArray Data Object
}
\description{
\code{str.H2OParsedDataVA}, a method for the \code{\link{str}} generic. Obtain information about H2O parsed data objects and their structure. 
}
\usage{
\method{str}{H2OParsedDataVA}(object, ...)
}
\arguments{
  \item{object}{An \code{\linkS4class{H2OParsedDataVA}} object.}
  \item{...}{Potential further arguments. (Currently unimplemented).}
}
\value{
A table listing summary information including variable names, types (for example, enum or numeric), count of observations and columns.
}

\examples{
library(h2o)
localH2O = h2o.init(ip = "localhost", port = 54321, startH2O = TRUE, silentUpgrade = TRUE, promptUpgrade = FALSE)
prosPath = system.file("extdata", "prostate.csv", package="h2oRClient")
prostate.hex = h2o.importFile.VA(localH2O, path = prosPath)
str(prostate.hex)
h2o.shutdown(localH2O)
}