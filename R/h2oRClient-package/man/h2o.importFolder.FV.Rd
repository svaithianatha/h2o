\name{h2o.importFolder.FV}
\alias{h2o.importFolder.FV}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
  Import Local Directory of Data Files to FluidVecs
}
\description{
  Imports all the files in the local directory and parses them, concatenating the data into a single FluidVecs frame and returning an object containing the identifying hex key.
}
\usage{
  h2o.importFolder.FV(object, path, pattern = "", key = "", parse = TRUE, header, sep = "", col.names)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{object}{
    An \code{\linkS4class{H2OClient}} object containing the IP address and port of the server running H2O.
  }
  \item{path}{
    The path of the folder directory to be imported. Each row of data appears as one line of the file. If it does not contain an absolute path, the file name is relative to the current working directory.
  }
  \item{pattern}{
(Optional) Character string containing a regular expression to match file(s) in the folder.
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
(Optional) An \code{\linkS4class{H2OParsedData}} object containing the column header as its first and only row.
  }
}
\details{
This method is identical to \code{\link{h2o.importFolder}} with input parameter \code{version = 2}.

This method imports all the data files in a given folder and concatenates them together row-wise into a single matrix represented by a \code{\linkS4class{H2OParsedData}} object. The data files must all have the same number of columns, and the columns must be lined up in the same order, otherwise an error will be returned.

WARNING: In H2O, import is lazy! Do not modify the data files on hard disk until after parsing is complete. By default, h2o.importFolder will automatically parse all files in the directory.
}
\value{
  If \code{parse = TRUE}, the function returns an object of class \code{\linkS4class{H2OParsedData}}, otherwise it returns an object of class \code{\linkS4class{H2ORawData}}.
  %%  ~Describe the value returned
  %%  If it is a LIST, use
  %%  \item{comp1 }{Description of 'comp1'}
  %%  \item{comp2 }{Description of 'comp2'}
  %% ...
}
\seealso{
  %% ~~objects to See Also as \code{\link{help}}, ~~~
  \code{\link{h2o.importFile.FV}, \link{h2o.importHDFS.FV}, \link{h2o.importURL.FV}, \link{h2o.uploadFile.FV}}
}
\examples{
library(h2o)
localH2O = h2o.init(ip = "localhost", port = 54321, startH2O = TRUE, silentUpgrade = TRUE, promptUpgrade = FALSE)
myPath = system.file("extdata", "prostate_folder", package = "h2oRClient")
prostate_all.hex = h2o.importFolder.FV(localH2O, path = myPath)
summary(prostate_all.hex)
h2o.shutdown(localH2O)
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
