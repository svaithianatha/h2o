\name{h2o.importFile.VA}
\alias{h2o.importFile.VA}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
Import Local Data File to ValueArray
}
\description{
Imports a file from the local path and parses it to ValueArray data, returning an object containing the identifying hex key. 
}
\usage{
h2o.importFile.VA(object, path, key = "", parse = TRUE, header, sep = "", col.names)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{object}{
An \code{\linkS4class{H2OClient}} object containing the IP address and port of the server running H2O.
}
  \item{path}{
The path of the file to be imported. Each row of data appears as one line of the file. If it does not contain an absolute path, the file name is relative to the current working directory.
}
  \item{key}{
(Optional) The unique hex key assigned to the imported file. If none is given, a key will automatically be generated based on the file path.
}
  \item{parse}{
(Optional) A logical value indicating whether the file should be parsed after import.
}
  \item{header}{
(Optional) A logical value indicating whether the first row is the column header. If missing, H2O will automatically try to detect the presence of a header.
  }
  \item{sep}{
(Optional) The field separator character. Values on each line of the file are separated by this character. If \code{sep = ""}, the parser will automatically detect the separator.
  }
  \item{col.names}{
(Optional) An \code{\linkS4class{H2OParsedDataVA}} object containing the column header as its first and only row.
  }
}
\details{
This method is identical to \code{\link{h2o.importFile}} with input parameter \code{version = 1}.

WARNING: In H2O, import is lazy! Do not modify the data on hard disk until after parsing is complete.
}
\value{
If \code{parse = TRUE}, the function returns an object of class \code{\linkS4class{H2OParsedDataVA}}, otherwise it returns an object of class \code{\linkS4class{H2ORawDataVA}}.
%%  ~Describe the value returned
%%  If it is a LIST, use
%%  \item{comp1 }{Description of 'comp1'}
%%  \item{comp2 }{Description of 'comp2'}
%% ...
}
\seealso{
%% ~~objects to See Also as \code{\link{help}}, ~~~
\code{\link{h2o.importFolder.VA}, \link{h2o.importHDFS.VA}, \link{h2o.importURL.VA}, \link{h2o.uploadFile.VA}}
}
\examples{
library(h2o)
localH2O = h2o.init(ip = "localhost", port = 54321, startH2O = TRUE, silentUpgrade = TRUE, promptUpgrade = FALSE)
irisPath = system.file("extdata", "iris.csv", package="h2oRClient")
iris.hex = h2o.importFile.VA(localH2O, path = irisPath, key = "iris.hex")
summary(iris.hex)
h2o.shutdown(localH2O)
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
